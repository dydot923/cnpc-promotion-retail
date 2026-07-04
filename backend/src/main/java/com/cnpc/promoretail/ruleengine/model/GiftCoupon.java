package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;

public record GiftCoupon(
        String couponName,
        BigDecimal amount
) {
}

