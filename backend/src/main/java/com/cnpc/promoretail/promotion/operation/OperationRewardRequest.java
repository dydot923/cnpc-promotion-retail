package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record OperationRewardRequest(
        @NotBlank String memberCode,
        LocalDate businessDate,
        String eventKey,
        String operatorId,
        String operatorName
) {
}
