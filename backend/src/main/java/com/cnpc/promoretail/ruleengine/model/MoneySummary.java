package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MoneySummary(
        BigDecimal originalAmount,
        BigDecimal payableAmount,
        BigDecimal discountAmount
) {

    public MoneySummary {
        originalAmount = money(originalAmount);
        payableAmount = money(payableAmount);
        discountAmount = money(discountAmount);
    }

    public static MoneySummary of(BigDecimal originalAmount, BigDecimal payableAmount, BigDecimal discountAmount) {
        return new MoneySummary(originalAmount, payableAmount, discountAmount);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

