package com.cnpc.promoretail.promotion.points;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PointsExchangeRequest(
        @NotNull @Min(1) Long pointsUsed,
        LocalDate businessDate,
        String stationCode,
        String operatorId,
        String operatorName
) {
}
