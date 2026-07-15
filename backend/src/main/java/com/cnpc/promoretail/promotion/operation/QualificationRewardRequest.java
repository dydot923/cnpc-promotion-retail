package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record QualificationRewardRequest(
        @NotBlank String memberCode,
        String qualificationType,
        LocalDate businessDate,
        String operatorId,
        String operatorName
) {
}
