package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record SignInRewardRequest(
        @NotBlank String memberCode,
        @Min(1) int signInDays,
        LocalDate businessDate,
        String operatorId,
        String operatorName
) {
}
