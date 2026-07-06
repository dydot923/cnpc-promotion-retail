package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CalculationResult(
        BigDecimal originalAmount,
        BigDecimal payableAmount,
        BigDecimal discountAmount,
        MoneySummary moneySummary,
        String recommendedCandidateId,
        List<PromotionCandidate> availableCandidates,
        List<BlockedPromotion> blockedPromotions,
        List<String> explanations,
        String ruleVersion,
        List<String> ruleVersionIds,
        List<InventoryWarning> inventoryWarnings,
        PromotionCandidate originalPriceFallback
) {

    public CalculationResult {
        originalAmount = money(originalAmount);
        payableAmount = money(payableAmount);
        discountAmount = money(discountAmount);
        moneySummary = moneySummary == null ? MoneySummary.of(originalAmount, payableAmount, discountAmount) : moneySummary;
        availableCandidates = availableCandidates == null ? List.of() : List.copyOf(availableCandidates);
        blockedPromotions = blockedPromotions == null ? List.of() : List.copyOf(blockedPromotions);
        explanations = explanations == null ? List.of() : List.copyOf(explanations);
        ruleVersionIds = ruleVersionIds == null ? List.of() : List.copyOf(ruleVersionIds);
        inventoryWarnings = inventoryWarnings == null ? List.of() : List.copyOf(inventoryWarnings);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
