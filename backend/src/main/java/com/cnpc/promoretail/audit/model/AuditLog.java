package com.cnpc.promoretail.audit.model;

import java.time.Instant;
import java.util.UUID;

public record AuditLog(
        String auditId,
        String actionType,
        String entityType,
        String entityId,
        Object beforeSnapshot,
        Object afterSnapshot,
        String operatorId,
        String operatorName,
        Instant operatedAt,
        String reason,
        Instant createdAt
) {

    public AuditLog {
        if (auditId == null || auditId.isBlank()) {
            throw new IllegalArgumentException("auditId is required");
        }
        if (actionType == null || actionType.isBlank()) {
            throw new IllegalArgumentException("actionType is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId is required");
        }
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
        operatedAt = operatedAt == null ? Instant.now() : operatedAt;
        reason = reason == null ? "" : reason;
        createdAt = createdAt == null ? operatedAt : createdAt;
    }

    public static AuditLog create(
            String actionType,
            String entityType,
            String entityId,
            Object beforeSnapshot,
            Object afterSnapshot,
            String operatorId,
            String operatorName,
            String reason
    ) {
        Instant now = Instant.now();
        return new AuditLog(
                "audit-" + UUID.randomUUID(),
                actionType,
                entityType,
                entityId,
                beforeSnapshot,
                afterSnapshot,
                operatorId,
                operatorName,
                now,
                reason,
                now
        );
    }
}
