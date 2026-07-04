package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CalculationResult(
        BigDecimal originalAmount,
        BigDecimal payableAmount,
        BigDecimal discountAmount,
        String recommendedCandidateId,
        List<PromotionCandidate> availableCandidates,
        List<BlockedPromotion> blockedPromotions,
        List<String> explanations,
        String ruleVersion,
        List<InventoryWarning> inventoryWarnings,
        PromotionCandidate originalPriceFallback
) {

    public CalculationResult {
        originalAmount = money(originalAmount);
        payableAmount = money(payableAmount);
        discountAmount = money(discountAmount);
        availableCandidates = availableCandidates == null ? List.of() : List.copyOf(availableCandidates);
        blockedPromotions = blockedPromotions == null ? List.of() : List.copyOf(blockedPromotions);
        explanations = explanations == null ? List.of() : List.copyOf(explanations);
        inventoryWarnings = inventoryWarnings == null ? List.of() : List.copyOf(inventoryWarnings);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

