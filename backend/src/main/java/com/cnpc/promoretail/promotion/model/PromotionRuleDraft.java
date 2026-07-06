package com.cnpc.promoretail.promotion.model;

import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.time.Instant;

public record PromotionRuleDraft(
        String draftId,
        PromotionRule rule,
        String sourceImportId,
        String sourceSheetName,
        int sourceRowNumber,
        PromotionRuleStatus status,
        boolean manualLocked,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {

    public PromotionRuleDraft {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("draftId is required");
        }
        if (rule == null) {
            throw new IllegalArgumentException("rule is required");
        }
        if (sourceImportId == null || sourceImportId.isBlank()) {
            throw new IllegalArgumentException("sourceImportId is required");
        }
        if (sourceSheetName == null || sourceSheetName.isBlank()) {
            throw new IllegalArgumentException("sourceSheetName is required");
        }
        if (sourceRowNumber <= 0) {
            throw new IllegalArgumentException("sourceRowNumber must be positive");
        }
        status = status == null ? PromotionRuleStatus.PENDING_CONFIRMATION : status;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        createdBy = createdBy == null || createdBy.isBlank() ? "system" : createdBy;
    }

    public PromotionRuleDraft withStatus(PromotionRuleStatus newStatus, boolean newManualLocked) {
        return new PromotionRuleDraft(draftId, rule.withStatus(newStatus), sourceImportId, sourceSheetName,
                sourceRowNumber, newStatus, newManualLocked, createdAt, Instant.now(), createdBy);
    }

    public PromotionRuleDraft withRule(PromotionRule newRule, PromotionRuleStatus newStatus, boolean newManualLocked) {
        return new PromotionRuleDraft(draftId, newRule.withStatus(newStatus), sourceImportId, sourceSheetName,
                sourceRowNumber, newStatus, newManualLocked, createdAt, Instant.now(), createdBy);
    }
}
