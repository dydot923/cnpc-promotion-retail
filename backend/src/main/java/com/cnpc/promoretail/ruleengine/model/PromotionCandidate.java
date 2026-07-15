package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

public record PromotionCandidate(
        String candidateId,
        String ruleId,
        String title,
        PromotionRuleType ruleType,
        BigDecimal originalAmount,
        BigDecimal payableAmount,
        BigDecimal discountAmount,
        List<GiftItem> gifts,
        List<GiftCoupon> coupons,
        String explanation,
        String ruleVersion,
        String exclusiveGroup,
        boolean stackable,
        int priority,
        Set<String> consumedProductCodes,
        Set<String> consumedCouponIds,
        List<CompositeBenefitComponent> compositeComponents,
        int pointsMultiplier
) {

    public PromotionCandidate {
        originalAmount = money(originalAmount);
        payableAmount = money(payableAmount);
        discountAmount = money(discountAmount);
        gifts = gifts == null ? List.of() : List.copyOf(gifts);
        coupons = coupons == null ? List.of() : List.copyOf(coupons);
        consumedProductCodes = consumedProductCodes == null ? Set.of() : Set.copyOf(consumedProductCodes);
        consumedCouponIds = consumedCouponIds == null ? Set.of() : Set.copyOf(consumedCouponIds);
        compositeComponents = compositeComponents == null ? List.of() : List.copyOf(compositeComponents);
        pointsMultiplier = pointsMultiplier <= 0 ? 1 : pointsMultiplier;
    }

    public PromotionCandidate(
            String candidateId,
            String ruleId,
            String title,
            PromotionRuleType ruleType,
            BigDecimal originalAmount,
            BigDecimal payableAmount,
            BigDecimal discountAmount,
            List<GiftItem> gifts,
            List<GiftCoupon> coupons,
            String explanation,
            String ruleVersion,
            String exclusiveGroup,
            boolean stackable,
            int priority,
            Set<String> consumedProductCodes,
            Set<String> consumedCouponIds,
            List<CompositeBenefitComponent> compositeComponents
    ) {
        this(candidateId, ruleId, title, ruleType, originalAmount, payableAmount, discountAmount, gifts,
                coupons, explanation, ruleVersion, exclusiveGroup, stackable, priority, consumedProductCodes,
                consumedCouponIds, compositeComponents, 1);
    }

    public PromotionCandidate(
            String candidateId,
            String ruleId,
            String title,
            PromotionRuleType ruleType,
            BigDecimal originalAmount,
            BigDecimal payableAmount,
            BigDecimal discountAmount,
            List<GiftItem> gifts,
            List<GiftCoupon> coupons,
            String explanation,
            String ruleVersion,
            String exclusiveGroup,
            boolean stackable,
            int priority,
            Set<String> consumedProductCodes,
            Set<String> consumedCouponIds
    ) {
        this(candidateId, ruleId, title, ruleType, originalAmount, payableAmount, discountAmount, gifts, coupons,
                explanation, ruleVersion, exclusiveGroup, stackable, priority, consumedProductCodes,
                consumedCouponIds, List.of(), 1);
    }

    public static PromotionCandidate originalPrice(BigDecimal originalAmount) {
        BigDecimal amount = money(originalAmount);
        return new PromotionCandidate(
                "original-price",
                "original-price",
                "原价结算",
                PromotionRuleType.ORIGINAL_PRICE,
                amount,
                amount,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                List.of(),
                List.of(),
                "未选择促销或无可用促销时，按原价结算。",
                "original",
                null,
                true,
                Integer.MAX_VALUE,
                Set.of(),
                Set.of(),
                List.of(),
                1
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
