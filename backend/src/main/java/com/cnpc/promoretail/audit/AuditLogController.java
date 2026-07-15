package com.cnpc.promoretail.audit;

import com.cnpc.promoretail.audit.model.AuditLog;
import com.cnpc.promoretail.audit.model.AuditLogQuery;
import com.cnpc.promoretail.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> search(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String operatorId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ApiResponse.ok(auditLogService.search(
                new AuditLogQuery(actionType, entityType, entityId, operatorId, limit)
        ));
    }
}
