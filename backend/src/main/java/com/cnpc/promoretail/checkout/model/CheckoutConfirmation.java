package com.cnpc.promoretail.checkout.model;

import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import java.time.Instant;

public record CheckoutConfirmation(
        String confirmationId,
        String calculationId,
        String selectedCandidateId,
        PromotionCandidate selectedCandidateSnapshot,
        String operatorId,
        String operatorName,
        boolean skipped,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public CheckoutConfirmation {
        if (confirmationId == null || confirmationId.isBlank()) {
            throw new IllegalArgumentException("confirmationId is required");
        }
        if (calculationId == null || calculationId.isBlank()) {
            throw new IllegalArgumentException("calculationId is required");
        }
        if (selectedCandidateId == null || selectedCandidateId.isBlank()) {
            throw new IllegalArgumentException("selectedCandidateId is required");
        }
        if (selectedCandidateSnapshot == null) {
            throw new IllegalArgumentException("selectedCandidateSnapshot is required");
        }
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
        confirmedAt = confirmedAt == null ? Instant.now() : confirmedAt;
        createdAt = createdAt == null ? confirmedAt : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }
}
