package com.cnpc.promoretail.audit;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuditLogService implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public DefaultAuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

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
        return auditLogRepository.save(AuditLog.create(
                actionType,
                entityType,
                entityId,
                beforeSnapshot,
                afterSnapshot,
                operatorId,
                operatorName,
                reason
        ));
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntity(entityType, entityId);
    }

    @Override
    public List<AuditLog> search(AuditLogQuery query) {
        return auditLogRepository.search(query);
    }
}
