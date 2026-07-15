package com.cnpc.promoretail.promotion.operation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record GroupBuyRewardRequest(
        @NotBlank String memberCode,
        @NotBlank String groupId,
        @Min(2) int groupSize,
        String memberRole,
        LocalDate businessDate,
        String operatorId,
        String operatorName
) {
}
