package com.cnpc.promoretail.promotion;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromotionRuleManagementController {

    private final PromotionRuleGovernanceService governanceService;

    public PromotionRuleManagementController(PromotionRuleGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/api/promotion-drafts")
    public ApiResponse<List<PromotionRuleDraft>> drafts(@RequestParam(required = false) PromotionRuleStatus status) {
        return ApiResponse.ok(governanceService.listDrafts(status));
    }

    @PostMapping("/api/promotion-drafts/{draftId}/confirm")
    public ApiResponse<PromotionRuleVersion> confirm(
            @PathVariable String draftId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.confirmDraft(draftId, operatorId(request), changeReason(request)));
    }

    @PostMapping("/api/promotion-drafts/{draftId}/reject")
    public ApiResponse<PromotionRuleDraft> reject(
            @PathVariable String draftId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.rejectDraft(draftId, operatorId(request), changeReason(request)));
    }

    @PostMapping("/api/promotion-rules/{ruleId}/disable")
    public ApiResponse<PromotionRuleVersion> disable(
            @PathVariable String ruleId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.disableRule(ruleId, operatorId(request), changeReason(request)));
    }

    @PostMapping("/api/promotion-rules/batch-confirm")
    public ApiResponse<List<PromotionRuleVersion>> batchConfirm(@RequestBody BatchRuleActionRequest request) {
        return ApiResponse.ok(governanceService.batchConfirmRules(
                request.ruleIds(),
                operatorId(request),
                changeReason(request)
        ));
    }

    @PostMapping("/api/promotion-rules/batch-revise")
    public ApiResponse<List<PromotionRuleDraft>> batchRevise(@RequestBody BatchRuleReviseRequest request) {
        return ApiResponse.ok(governanceService.batchReviseRules(
                request.revisions(),
                operatorId(request),
                changeReason(request)
        ));
    }

    @PostMapping("/api/promotion-rules/batch-archive")
    public ApiResponse<List<PromotionRuleVersion>> batchArchive(@RequestBody BatchRuleActionRequest request) {
        return ApiResponse.ok(governanceService.batchArchiveRules(
                request.ruleIds(),
                operatorId(request),
                changeReason(request)
        ));
    }

    @GetMapping("/api/promotion-rules/{ruleId}/audit-logs")
    public ApiResponse<List<PromotionRuleAuditLog>> auditLogs(@PathVariable String ruleId) {
        return ApiResponse.ok(governanceService.auditLogs(ruleId));
    }

    private String operatorId(RuleActionRequest request) {
        return request == null || request.operatorId() == null || request.operatorId().isBlank()
                ? "frontend-mvp"
                : request.operatorId();
    }

    private String operatorId(BatchRuleActionRequest request) {
        return request == null || request.operatorId() == null || request.operatorId().isBlank()
                ? "frontend-mvp"
                : request.operatorId();
    }

    private String operatorId(BatchRuleReviseRequest request) {
        return request == null || request.operatorId() == null || request.operatorId().isBlank()
                ? "frontend-mvp"
                : request.operatorId();
    }

    private String changeReason(RuleActionRequest request) {
        return request == null || request.changeReason() == null ? "" : request.changeReason();
    }

    private String changeReason(BatchRuleActionRequest request) {
        return request == null || request.changeReason() == null ? "" : request.changeReason();
    }

    private String changeReason(BatchRuleReviseRequest request) {
        return request == null || request.changeReason() == null ? "" : request.changeReason();
    }

    public record RuleActionRequest(String operatorId, String changeReason) {
    }

    public record BatchRuleActionRequest(List<String> ruleIds, String operatorId, String changeReason) {
    }

    public record BatchRuleReviseRequest(
            List<PromotionRuleReviseRequest> revisions,
            String operatorId,
            String changeReason
    ) {
    }
}
