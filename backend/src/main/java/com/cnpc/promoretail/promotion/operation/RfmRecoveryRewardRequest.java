package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RfmRecoveryRewardRequest(
        @NotBlank String memberCode,
        String customerType,
        LocalDate businessDate,
        String operatorId,
        String operatorName
) {
}
