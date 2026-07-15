package com.cnpc.promoretail.checkout;

import jakarta.validation.constraints.NotBlank;

public record CheckoutConfirmRequest(
        String orderNo,
        @NotBlank String calculationId,
        @NotBlank String selectedCandidateId,
        boolean skippedPromotion,
        String operatorId,
        String operatorName
) {
}
