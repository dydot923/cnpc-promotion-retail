package com.cnpc.promoretail.checkout;

import jakarta.validation.constraints.NotBlank;

public record CheckoutConfirmRequest(
        @NotBlank String orderNo,
        @NotBlank String selectedCandidateId,
        boolean skippedPromotion,
        String operatorId
) {
}

