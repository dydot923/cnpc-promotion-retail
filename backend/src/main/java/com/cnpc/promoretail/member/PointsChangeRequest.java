package com.cnpc.promoretail.member;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PointsChangeRequest(
        @NotBlank String changeType,
        @Min(1) long amount,
        String reason
) {
}
