package com.cnpc.promoretail.promotion.model;

import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;

public record ImportedPromotionRule(
        ImportVersion importId,
        String sourceSheetName,
        int sourceRowNumber,
        PromotionRule rule
) {

    public ImportedPromotionRule {
        if (importId == null) {
            throw new IllegalArgumentException("importId is required");
        }
        if (sourceSheetName == null || sourceSheetName.isBlank()) {
            throw new IllegalArgumentException("sourceSheetName is required");
        }
        if (sourceRowNumber <= 0) {
            throw new IllegalArgumentException("sourceRowNumber must be positive");
        }
        if (rule == null) {
            throw new IllegalArgumentException("rule is required");
        }
    }
}
