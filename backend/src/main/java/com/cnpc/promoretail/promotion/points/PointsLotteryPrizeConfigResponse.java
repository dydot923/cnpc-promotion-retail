package com.cnpc.promoretail.promotion.points;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PointsLotteryPrizeConfigResponse(
        String prizeId,
        String activityCode,
        String prizeName,
        String prizeType,
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        int validDays,
        int weight,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PointsLotteryPrizeConfigResponse from(PointsLotteryPrizeConfig config) {
        return new PointsLotteryPrizeConfigResponse(
                config.prizeId(),
                config.activityCode(),
                config.prizeName(),
                config.prizeType(),
                config.couponTemplateId(),
                config.couponName(),
                config.faceValue(),
                config.minSpendAmount(),
                config.applicableCategories(),
                config.excludedCategories(),
                config.validDays(),
                config.weight(),
                config.status(),
                config.createdAt(),
                config.updatedAt()
        );
    }
}
