package com.cnpc.promoretail.promotion.points;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PointsLotteryPrizeConfigRequest(
        String prizeId,
        String activityCode,
        @NotBlank String prizeName,
        @NotBlank String prizeType,
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        @Min(1) Integer validDays,
        @Min(0) Integer weight,
        String status
) {

    public PointsLotteryPrizeConfig toConfig(String pathPrizeId) {
        String effectivePrizeId = pathPrizeId == null || pathPrizeId.isBlank()
                ? requestPrizeId()
                : pathPrizeId.trim();
        Instant now = Instant.now();
        return new PointsLotteryPrizeConfig(
                effectivePrizeId,
                activityCode,
                prizeName,
                prizeType,
                couponTemplateId,
                couponName,
                faceValue,
                minSpendAmount,
                applicableCategories,
                excludedCategories,
                validDays == null ? 30 : validDays,
                weight == null ? 0 : weight,
                status,
                now,
                now
        );
    }

    private String requestPrizeId() {
        return prizeId == null || prizeId.isBlank()
                ? "lottery-prize-" + UUID.randomUUID()
                : prizeId.trim();
    }
}
