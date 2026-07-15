package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;

public record GiftCoupon(
        String couponName,
        BigDecimal amount,
        int quantity,
        BigDecimal useThreshold,
        int validDays,
        String couponTemplateId
) {

    public GiftCoupon {
        couponTemplateId = couponTemplateId == null ? "" : couponTemplateId;
        amount = amount == null ? BigDecimal.ZERO : amount;
        quantity = quantity <= 0 ? 1 : quantity;
        useThreshold = useThreshold == null ? BigDecimal.ZERO : useThreshold;
        validDays = Math.max(validDays, 0);
    }

    public GiftCoupon(
            String couponName,
            BigDecimal amount,
            int quantity,
            BigDecimal useThreshold,
            int validDays
    ) {
        this(couponName, amount, quantity, useThreshold, validDays, "");
    }
}
