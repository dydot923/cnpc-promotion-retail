package com.cnpc.promoretail.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditLogServiceTest {

    private final InMemoryAuditLogRepository repository = new InMemoryAuditLogRepository();
    private final AuditLogService auditLogService = new DefaultAuditLogService(repository);

    @Test
    void searchFiltersByActionEntityAndOperator() {
        AuditLog first = audit("audit-1", "CHECKOUT_CONFIRM", "CHECKOUT_CONFIRMATION",
                "confirm-1", "cashier-a", Instant.parse("2026-07-07T01:00:00Z"));
        AuditLog second = audit("audit-2", "REPLENISHMENT_EXPORT", "REPLENISHMENT_LIST",
                "repl-1", "stock-a", Instant.parse("2026-07-07T02:00:00Z"));
        AuditLog third = audit("audit-3", "CHECKOUT_CONFIRM", "CHECKOUT_CONFIRMATION",
                "confirm-2", "cashier-b", Instant.parse("2026-07-07T03:00:00Z"));
        repository.save(first);
        repository.save(second);
        repository.save(third);

        assertThat(auditLogService.search(new AuditLogQuery("CHECKOUT_CONFIRM", null, null, null, 10)))
                .extracting(AuditLog::auditId)
                .containsExactly("audit-3", "audit-1");
        assertThat(auditLogService.search(new AuditLogQuery(null, "REPLENISHMENT_LIST", "repl-1", "stock-a", 10)))
                .extracting(AuditLog::auditId)
                .containsExactly("audit-2");
        assertThat(auditLogService.search(new AuditLogQuery(null, null, null, null, 2)))
                .extracting(AuditLog::auditId)
                .containsExactly("audit-3", "audit-2");
    }

    private AuditLog audit(
            String auditId,
            String actionType,
            String entityType,
            String entityId,
            String operatorId,
            Instant operatedAt
    ) {
        return new AuditLog(auditId, actionType, entityType, entityId,
                null, Map.of("id", entityId), operatorId, "", operatedAt, "", operatedAt);
    }
}
