package com.cnpc.promoretail.promotion.coupon;

public record CouponStatsResponse(
        int total,
        int available,
        int used,
        int expired,
        int disabled
) {
}
