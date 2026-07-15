package com.cnpc.promoretail.audit.repository;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import java.util.List;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEntity(String entityType, String entityId);

    List<AuditLog> search(AuditLogQuery query);
}
