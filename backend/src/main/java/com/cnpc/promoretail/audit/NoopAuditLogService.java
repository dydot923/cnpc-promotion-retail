package com.cnpc.promoretail.audit;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import java.util.List;

final class NoopAuditLogService implements AuditLogService {

    @Override
    public AuditLog record(
            String actionType,
            String entityType,
            String entityId,
            Object beforeSnapshot,
            Object afterSnapshot,
            String operatorId,
            String operatorName,
            String reason
    ) {
        return AuditLog.create(actionType, entityType, entityId, beforeSnapshot, afterSnapshot,
                operatorId, operatorName, reason);
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, String entityId) {
        return List.of();
    }

    @Override
    public List<AuditLog> search(AuditLogQuery query) {
        return List.of();
    }
}
