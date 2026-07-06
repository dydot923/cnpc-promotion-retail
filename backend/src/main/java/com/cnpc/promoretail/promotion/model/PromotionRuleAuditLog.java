package com.cnpc.promoretail.promotion.model;

import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.time.Instant;

public record PromotionRuleAuditLog(
        String auditId,
        String ruleId,
        PromotionRuleAuditAction action,
        PromotionRuleStatus statusBefore,
        PromotionRuleStatus statusAfter,
        String operatorId,
        String changeReason,
        Instant createdAt
) {

    public PromotionRuleAuditLog {
        if (auditId == null || auditId.isBlank()) {
            throw new IllegalArgumentException("auditId is required");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId is required");
        }
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        changeReason = changeReason == null ? "" : changeReason;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
