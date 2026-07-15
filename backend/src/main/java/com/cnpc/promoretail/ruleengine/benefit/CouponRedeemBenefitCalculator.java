package com.cnpc.promoretail.ruleengine.benefit;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CartTotals;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CouponRedeemBenefitCalculator extends AbstractBenefitCalculator {

    @Override
    public boolean supports(PromotionRuleType type) {
        return type == PromotionRuleType.COUPON_REDEEM;
    }

    @Override
    public BenefitCalculation calculate(OrderContext context, PromotionRule rule, CartTotals totals) {
        if (context.availableCoupons().isEmpty()) {
            return BenefitCalculation.blocked(List.of("当前顾客没有可核销券"));
        }

        List<PromotionCandidate> candidates = new ArrayList<>();
        List<String> blockedReasons = new ArrayList<>();
        for (Coupon coupon : context.availableCoupons()) {
            List<String> reasons = validateCoupon(context, coupon);
            if (!reasons.isEmpty()) {
                blockedReasons.add(coupon.couponId() + "：" + String.join("；", reasons));
                continue;
            }

            List<CartItem> applicableItems = applicableItems(context, coupon);
            BigDecimal applicableSubtotal = eligibleSubtotal(applicableItems);
            BigDecimal discount = coupon.discountRate().compareTo(BigDecimal.ZERO) > 0
                    ? money(applicableSubtotal.multiply(BigDecimal.ONE.subtract(coupon.discountRate())))
                    : money(coupon.faceValue().min(applicableSubtotal));
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                blockedReasons.add(coupon.couponId() + "：券抵扣金额必须大于0");
                continue;
            }
            BigDecimal payable = money(totals.originalAmount().subtract(discount).max(BigDecimal.ZERO));
            candidates.add(new PromotionCandidate(
                    "cand-" + rule.ruleId() + "-" + coupon.couponId(),
                    rule.ruleId(),
                    coupon.couponName(),
                    rule.ruleType(),
                    totals.originalAmount(),
                    payable,
                    discount,
                    List.of(),
                    List.of(),
                    couponExplanation(coupon, discount),
                    rule.version(),
                    coupon.stackable() ? null : directDiscountGroup(rule),
                    coupon.stackable(),
                    rule.priority(),
                    consumedProductCodes(rule, applicableItems),
                    Set.of(coupon.couponId())
            ));
        }

        if (candidates.isEmpty()) {
            return BenefitCalculation.blocked(blockedReasons);
        }
        return BenefitCalculation.mixed(candidates, blockedReasons);
    }

    private String couponExplanation(Coupon coupon, BigDecimal discount) {
        if (coupon.discountRate().compareTo(BigDecimal.ZERO) > 0) {
            return "使用" + coupon.couponName() + "，满" + money(coupon.minSpendAmount())
                    + "元可用，按" + coupon.discountRate() + "折扣率核销，抵扣" + discount + "元";
        }
        return "使用" + coupon.couponName() + "，满" + money(coupon.minSpendAmount())
                + "元可用，抵扣" + discount + "元";
    }

    private List<String> validateCoupon(OrderContext context, Coupon coupon) {
        List<String> reasons = new ArrayList<>();
        LocalDate businessDate = context.transactionDate();
        if (coupon.status() != CouponStatus.AVAILABLE) {
            reasons.add("券状态不是AVAILABLE");
        }
        if (businessDate != null && coupon.validFrom() != null && businessDate.isBefore(coupon.validFrom())) {
            reasons.add("券未到有效期");
        }
        if (businessDate != null && coupon.validUntil() != null && businessDate.isAfter(coupon.validUntil())) {
            reasons.add("券已过期");
        }
        if (coupon.memberOnly() && !context.customer().member()) {
            reasons.add("券为会员专属");
        }
        if (!coupon.sequenceGroup().isBlank() && coupon.sequenceOrder() != null) {
            reasons.addAll(validateSequenceCoupon(context, coupon));
        }
        if (containsExcludedProduct(context, coupon)) {
            reasons.add("券排除商品命中");
        }
        if (containsExcludedCategory(context, coupon)) {
            reasons.add("券排除品类命中");
        }
        List<CartItem> applicableItems = applicableItems(context, coupon);
        if (applicableItems.isEmpty()) {
            reasons.add("券适用范围不匹配");
            return reasons;
        }
        BigDecimal applicableSubtotal = eligibleSubtotal(applicableItems);
        if (coupon.minSpendAmount().compareTo(BigDecimal.ZERO) > 0
                && applicableSubtotal.compareTo(coupon.minSpendAmount()) < 0) {
            reasons.add("未满" + money(coupon.minSpendAmount()) + "元");
        }
        return reasons;
    }

    private List<String> validateSequenceCoupon(OrderContext context, Coupon coupon) {
        List<String> reasons = new ArrayList<>();
        int sequenceOrder = coupon.sequenceOrder();
        for (int requiredOrder = 1; requiredOrder < sequenceOrder; requiredOrder++) {
            int order = requiredOrder;
            boolean previousUsed = context.availableCoupons().stream()
                    .filter(candidate -> coupon.sequenceGroup().equals(candidate.sequenceGroup()))
                    .filter(candidate -> candidate.sequenceOrder() != null && candidate.sequenceOrder() == order)
                    .anyMatch(candidate -> candidate.status() == CouponStatus.USED);
            if (!previousUsed) {
                reasons.add("需先核销第" + requiredOrder + "张券");
            }
        }
        if (sequenceOrder >= 3 && !context.customer().eEnjoyCardPayment()) {
            reasons.add("第3张及以后序列券仅限e享卡支付");
        }
        return reasons;
    }

    private boolean containsExcludedProduct(OrderContext context, Coupon coupon) {
        return context.cartItems().stream()
                .anyMatch(item -> coupon.excludedProductCodes().contains(item.productCode()));
    }

    private boolean containsExcludedCategory(OrderContext context, Coupon coupon) {
        return context.cartItems().stream()
                .anyMatch(item -> item.category() != null && coupon.excludedCategories().contains(item.category()));
    }

    private List<CartItem> applicableItems(OrderContext context, Coupon coupon) {
        return context.cartItems().stream()
                .filter(item -> coupon.applicableProductCodes().isEmpty()
                        || coupon.applicableProductCodes().contains(item.productCode()))
                .filter(item -> coupon.applicableCategories().isEmpty()
                        || coupon.applicableCategories().contains(item.category()))
                .filter(item -> !coupon.excludedProductCodes().contains(item.productCode()))
                .filter(item -> item.category() == null || !coupon.excludedCategories().contains(item.category()))
                .toList();
    }

    private String directDiscountGroup(PromotionRule rule) {
        return rule.exclusiveGroup() == null || rule.exclusiveGroup().isBlank()
                ? "direct_discount"
                : rule.exclusiveGroup();
    }

    @Override
    protected Set<String> consumedProductCodes(PromotionRule rule, List<CartItem> items) {
        if (items != null && !items.isEmpty()) {
            return items.stream().map(CartItem::productCode).collect(Collectors.toUnmodifiableSet());
        }
        return super.consumedProductCodes(rule, items);
    }
}
