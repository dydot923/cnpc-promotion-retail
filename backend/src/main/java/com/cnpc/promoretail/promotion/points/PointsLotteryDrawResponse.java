package com.cnpc.promoretail.promotion.points;

import com.cnpc.promoretail.member.MemberCouponResponse;
import java.time.Instant;
import java.time.LocalDate;

public record PointsLotteryDrawResponse(
        String drawId,
        String memberCode,
        String activityCode,
        int pointsCost,
        long availablePointsAfter,
        String prizeType,
        String resultLabel,
        MemberCouponResponse prizeCoupon,
        LocalDate businessDate,
        Instant createdAt
) {

    public static PointsLotteryDrawResponse from(PointsLotteryDraw draw, long availablePointsAfter, MemberCouponResponse coupon) {
        return new PointsLotteryDrawResponse(
                draw.drawId(),
                draw.memberCode(),
                draw.activityCode(),
                draw.pointsCost(),
                availablePointsAfter,
                draw.prizeType(),
                draw.resultLabel(),
                coupon,
                draw.businessDate(),
                draw.createdAt()
        );
    }
}
