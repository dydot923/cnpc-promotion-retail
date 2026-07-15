package com.cnpc.promoretail.audit;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import java.util.List;

public interface AuditLogService {

    AuditLog record(
            String actionType,
            String entityType,
            String entityId,
            Object beforeSnapshot,
            Object afterSnapshot,
            String operatorId,
            String operatorName,
            String reason
    );

    List<AuditLog> findByEntity(String entityType, String entityId);

    List<AuditLog> search(AuditLogQuery query);

    static AuditLogService noop() {
        return new NoopAuditLogService();
    }
}
