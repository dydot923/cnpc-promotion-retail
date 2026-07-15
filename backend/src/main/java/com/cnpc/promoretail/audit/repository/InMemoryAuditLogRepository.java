package com.cnpc.promoretail.audit.repository;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final ConcurrentMap<String, AuditLog> logs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> insertionOrder = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public AuditLog save(AuditLog auditLog) {
        insertionOrder.computeIfAbsent(auditLog.auditId(), ignored -> sequence.incrementAndGet());
        logs.put(auditLog.auditId(), auditLog);
        return auditLog;
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, String entityId) {
        return logs.values().stream()
                .filter(log -> log.entityType().equals(entityType))
                .filter(log -> log.entityId().equals(entityId))
                .sorted(Comparator.comparing(AuditLog::operatedAt)
                        .thenComparingLong(log -> insertionOrder.getOrDefault(log.auditId(), Long.MAX_VALUE)))
                .toList();
    }

    @Override
    public List<AuditLog> search(AuditLogQuery query) {
        return logs.values().stream()
                .filter(log -> query.actionType() == null || log.actionType().equals(query.actionType()))
                .filter(log -> query.entityType() == null || log.entityType().equals(query.entityType()))
                .filter(log -> query.entityId() == null || log.entityId().equals(query.entityId()))
                .filter(log -> query.operatorId() == null || log.operatorId().equals(query.operatorId()))
                .sorted(Comparator.comparing(AuditLog::operatedAt)
                        .thenComparingLong(log -> insertionOrder.getOrDefault(log.auditId(), Long.MIN_VALUE))
                        .reversed())
                .limit(query.limit())
                .toList();
    }
}
