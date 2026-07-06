package com.cnpc.promoretail.promotion.model;

import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.time.Instant;

public record PromotionRuleVersion(
        String versionId,
        String ruleId,
        String sourceImportId,
        String sourceSheetName,
        int sourceRowNumber,
        PromotionRuleType ruleType,
        PromotionRuleStatus status,
        Instant createdAt,
        String createdBy,
        Instant confirmedAt,
        String confirmedBy,
        String changeReason,
        PromotionRule rule
) {

    public PromotionRuleVersion {
        if (versionId == null || versionId.isBlank()) {
            throw new IllegalArgumentException("versionId is required");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (sourceImportId == null || sourceImportId.isBlank()) {
            throw new IllegalArgumentException("sourceImportId is required");
        }
        if (sourceSheetName == null || sourceSheetName.isBlank()) {
            throw new IllegalArgumentException("sourceSheetName is required");
        }
        if (ruleType == null) {
            throw new IllegalArgumentException("ruleType is required");
        }
        status = status == null ? PromotionRuleStatus.PENDING_CONFIRMATION : status;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        createdBy = createdBy == null || createdBy.isBlank() ? "system" : createdBy;
        if (rule == null) {
            throw new IllegalArgumentException("rule is required");
        }
    }
}
