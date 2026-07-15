package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CompositeBenefitComponent(
        PromotionRuleType type,
        String description,
        BigDecimal amount,
        int quantity,
        BigDecimal useThreshold,
        int validDays
) {

    public CompositeBenefitComponent {
        if (type != PromotionRuleType.AMOUNT_OFF && type != PromotionRuleType.GIFT_COUPON) {
            throw new IllegalArgumentException("composite component only supports amount off or gift coupon");
        }
        description = description == null ? "" : description;
        amount = money(amount);
        quantity = quantity <= 0 ? 1 : quantity;
        useThreshold = money(useThreshold);
        validDays = Math.max(validDays, 0);
    }

    public static CompositeBenefitComponent amountOff(BigDecimal amount) {
        return new CompositeBenefitComponent(PromotionRuleType.AMOUNT_OFF, "满减", amount, 1, BigDecimal.ZERO, 0);
    }

    public static CompositeBenefitComponent giftCoupon(
            String couponName,
            BigDecimal amount,
            int quantity,
            BigDecimal useThreshold,
            int validDays
    ) {
        return new CompositeBenefitComponent(PromotionRuleType.GIFT_COUPON, couponName, amount, quantity,
                useThreshold, validDays);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
