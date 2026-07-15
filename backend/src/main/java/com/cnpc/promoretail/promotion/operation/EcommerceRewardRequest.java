package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EcommerceRewardRequest(
        @NotBlank String memberCode,
        String rewardCode,
        @Min(1) @Max(20) Integer quantity,
        LocalDate businessDate,
        String eventKey,
        String operatorId,
        String operatorName
) {
}
