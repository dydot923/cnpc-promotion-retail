package com.cnpc.promoretail.audit.model;

public record AuditLogQuery(
        String actionType,
        String entityType,
        String entityId,
        String operatorId,
        int limit
) {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    public AuditLogQuery {
        actionType = blankToNull(actionType);
        entityType = blankToNull(entityType);
        entityId = blankToNull(entityId);
        operatorId = blankToNull(operatorId);
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        limit = Math.min(limit, MAX_LIMIT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
