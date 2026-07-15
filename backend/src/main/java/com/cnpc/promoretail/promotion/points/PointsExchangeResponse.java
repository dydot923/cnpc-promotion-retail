package com.cnpc.promoretail.promotion.points;

import com.cnpc.promoretail.member.MemberCouponResponse;
import java.time.LocalDate;

public record PointsExchangeResponse(
        String exchangeId,
        String memberCode,
        long pointsUsed,
        long availablePointsAfter,
        LocalDate businessDate,
        MemberCouponResponse coupon
) {
}
