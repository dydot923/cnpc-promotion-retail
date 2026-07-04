package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
        int priority
) {

    public PromotionCandidate {
        originalAmount = money(originalAmount);
        payableAmount = money(payableAmount);
        discountAmount = money(discountAmount);
        gifts = gifts == null ? List.of() : List.copyOf(gifts);
        coupons = coupons == null ? List.of() : List.copyOf(coupons);
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
                Integer.MIN_VALUE
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

