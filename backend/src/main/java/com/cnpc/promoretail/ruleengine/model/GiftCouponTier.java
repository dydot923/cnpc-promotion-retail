package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.util.List;

public record GiftCouponTier(
        BigDecimal thresholdAmount,
        List<GiftCoupon> coupons
) {

    public GiftCouponTier {
        thresholdAmount = thresholdAmount == null ? BigDecimal.ZERO : thresholdAmount;
        coupons = coupons == null ? List.of() : List.copyOf(coupons);
    }
}
