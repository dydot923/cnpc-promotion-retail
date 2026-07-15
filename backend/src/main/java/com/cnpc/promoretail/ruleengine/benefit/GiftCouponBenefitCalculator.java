package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.GiftCouponTier;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class GiftCouponBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.GIFT_COUPON;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty() && requiresCartTrigger(rule.condition())) {
            return BenefitCalculation.blocked(List.of("没有满足赠券条件的商品。"));
        }
        BigDecimal eligibleSubtotal = eligibleSubtotal(items);
        if (!items.isEmpty()
                && rule.condition().minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && eligibleSubtotal.compareTo(rule.condition().minCartAmount()) < 0) {
            return BenefitCalculation.blocked(List.of("适用商品金额未达到赠券门槛。"));
        }
        if (!rule.benefit().giftCouponTiers().isEmpty()) {
            return calculateTieredGiftCoupons(context, rule, totals, items, eligibleSubtotal);
        }
        if (rule.benefit().giftCouponName() == null || rule.benefit().giftCouponName().isBlank()) {
            return BenefitCalculation.blocked(List.of("赠券信息不完整。"));
        }
        if (rule.benefit().giftCouponAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BenefitCalculation.blocked(List.of("赠券面额必须大于 0。"));
        }

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                totals.originalAmount(),
                BigDecimal.ZERO,
                List.of(),
                List.of(new GiftCoupon(rule.benefit().giftCouponName(), rule.benefit().giftCouponAmount(),
                        rule.benefit().giftCouponQuantity(), rule.benefit().giftCouponUseThreshold(),
                        rule.benefit().giftCouponValidDays())),
                "满足赠券条件，应提示发放电子券。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority(),
                consumedProductCodes(rule, items),
                java.util.Set.of()
        );
        return BenefitCalculation.available(candidate);
    }

    private BenefitCalculation calculateTieredGiftCoupons(
            OrderContext context,
            PromotionRule rule,
            CartTotals totals,
            List<CartItem> items,
            BigDecimal eligibleSubtotal
    ) {
        BigDecimal basisAmount = basisAmount(context, eligibleSubtotal);
        GiftCouponTier tier = rule.benefit().giftCouponTiers().stream()
                .filter(item -> item.thresholdAmount().compareTo(basisAmount) <= 0)
                .max(Comparator.comparing(GiftCouponTier::thresholdAmount))
                .orElse(null);
        if (tier == null || tier.coupons().isEmpty()) {
            return BenefitCalculation.blocked(List.of("消费金额未达到任何赠券档位。"));
        }

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                totals.originalAmount(),
                BigDecimal.ZERO,
                List.of(),
                tier.coupons(),
                "满足赠券档位，消费金额 " + money(basisAmount) + " 元，发放 "
                        + tier.coupons().size() + " 类电子券。",
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority(),
                consumedProductCodes(rule, items),
                java.util.Set.of()
        );
        return BenefitCalculation.available(candidate);
    }

    private boolean requiresCartTrigger(PromotionCondition condition) {
        return !condition.productCodes().isEmpty()
                || !condition.includedCategories().isEmpty()
                || condition.minCartAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal basisAmount(OrderContext context, BigDecimal eligibleSubtotal) {
        if (context.rechargeAmount().compareTo(BigDecimal.ZERO) > 0) {
            return context.rechargeAmount();
        }
        if (context.fuel().amount().compareTo(BigDecimal.ZERO) > 0) {
            return context.fuel().amount();
        }
        return eligibleSubtotal;
    }
}
