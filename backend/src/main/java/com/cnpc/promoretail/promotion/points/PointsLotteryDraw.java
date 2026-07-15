package com.cnpc.promoretail.promotion.points;

import java.time.Instant;
import java.time.LocalDate;

public record PointsLotteryDraw(
        String drawId,
        String memberCode,
        String activityCode,
        int pointsCost,
        String prizeType,
        String prizeCouponId,
        String resultLabel,
        LocalDate businessDate,
        String stationCode,
        String operatorId,
        String operatorName,
        Instant createdAt
) {

    public PointsLotteryDraw {
        if (drawId == null || drawId.isBlank()) {
            throw new IllegalArgumentException("drawId is required");
        }
        if (memberCode == null || memberCode.isBlank()) {
            throw new IllegalArgumentException("memberCode is required");
        }
        activityCode = activityCode == null || activityCode.isBlank()
                ? "activity-board-v2-g2-points-lottery"
                : activityCode;
        if (pointsCost <= 0) {
            pointsCost = 500;
        }
        prizeType = prizeType == null || prizeType.isBlank() ? "NO_PRIZE" : prizeType;
        prizeCouponId = prizeCouponId == null ? "" : prizeCouponId;
        resultLabel = resultLabel == null ? "" : resultLabel;
        businessDate = businessDate == null ? LocalDate.now() : businessDate;
        stationCode = stationCode == null ? "" : stationCode;
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
