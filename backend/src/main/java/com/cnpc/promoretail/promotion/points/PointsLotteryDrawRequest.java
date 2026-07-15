package com.cnpc.promoretail.promotion.points;

import java.time.LocalDate;

public record PointsLotteryDrawRequest(
        LocalDate businessDate,
        String stationCode,
        String operatorId,
        String operatorName
) {
}
