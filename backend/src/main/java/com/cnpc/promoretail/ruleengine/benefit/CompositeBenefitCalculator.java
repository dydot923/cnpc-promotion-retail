package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.CompositeBenefitComponent;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompositeBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.COMPOSITE;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        List<CartItem> items = eligibleItems(context, rule);
        if (items.isEmpty()) {
            return BenefitCalculation.blocked(List.of("没有满足复合促销条件的商品。"));
        }

        BigDecimal eligibleSubtotal = eligibleSubtotal(items);
        if (rule.condition().minCartAmount().compareTo(BigDecimal.ZERO) > 0
                && eligibleSubtotal.compareTo(rule.condition().minCartAmount()) < 0) {
            return BenefitCalculation.blocked(List.of("适用商品金额未达到复合促销门槛。"));
        }

        List<CompositeBenefitComponent> components = rule.benefit().compositeComponents();
        if (components.isEmpty()) {
            return BenefitCalculation.blocked(List.of("复合促销没有配置优惠组件。"));
        }

        BigDecimal requestedAmountOff = components.stream()
                .filter(component -> component.type() == PromotionRuleType.AMOUNT_OFF)
                .map(CompositeBenefitComponent::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = money(requestedAmountOff.min(eligibleSubtotal));

        List<GiftCoupon> coupons = new ArrayList<>();
        for (CompositeBenefitComponent component : components) {
            if (component.type() != PromotionRuleType.GIFT_COUPON) {
                continue;
            }
            if (component.amount().compareTo(BigDecimal.ZERO) <= 0 || component.description().isBlank()) {
                return BenefitCalculation.blocked(List.of("复合促销赠券组件配置不完整。"));
            }
            coupons.add(new GiftCoupon(component.description(), component.amount(), component.quantity(),
                    component.useThreshold(), component.validDays()));
        }
        if (discount.compareTo(BigDecimal.ZERO) <= 0 && coupons.isEmpty()) {
            return BenefitCalculation.blocked(List.of("复合促销未产生可执行优惠。"));
        }

        BigDecimal payable = money(totals.originalAmount().subtract(discount).max(BigDecimal.ZERO));
        String couponText = coupons.isEmpty()
                ? ""
                : "，并赠送" + coupons.stream()
                .map(coupon -> coupon.quantity() + "张" + coupon.amount() + "元" + coupon.couponName())
                .collect(Collectors.joining("、"));
        String explanation = "命中复合促销：满减" + discount + "元" + couponText;

        PromotionCandidate candidate = new PromotionCandidate(
                "cand-" + rule.ruleId(),
                rule.ruleId(),
                rule.activityName(),
                rule.ruleType(),
                totals.originalAmount(),
                payable,
                discount,
                List.of(),
                coupons,
                explanation,
                rule.version(),
                rule.exclusiveGroup(),
                rule.stackable(),
                rule.priority(),
                consumedProductCodes(rule, items),
                Set.of(),
                components
        );
        return BenefitCalculation.available(candidate);
    }
}
