package com.cnpc.promoretail.promotion.excludedcategory;

public record PromotionExcludedCategory(
        String ruleId,
        String categoryName,
        String reason
) {

    public PromotionExcludedCategory {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("categoryName is required");
        }
        reason = reason == null ? "" : reason;
    }
}
