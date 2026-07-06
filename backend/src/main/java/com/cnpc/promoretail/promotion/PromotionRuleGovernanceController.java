package com.cnpc.promoretail.promotion;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionRuleGovernanceController {

    private final PromotionRuleGovernanceService governanceService;

    public PromotionRuleGovernanceController(PromotionRuleGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @PostMapping("/drafts/{draftId}/confirm")
    public ApiResponse<PromotionRuleVersion> confirmDraft(
            @PathVariable String draftId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.confirmDraft(draftId, operatorId(request), changeReason(request)));
    }

    @PostMapping("/drafts/{draftId}/reject")
    public ApiResponse<PromotionRuleDraft> rejectDraft(
            @PathVariable String draftId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.rejectDraft(draftId, operatorId(request), changeReason(request)));
    }

    @PostMapping("/drafts/{draftId}/revise")
    public ApiResponse<PromotionRuleDraft> reviseDraft(
            @PathVariable String draftId,
            @Valid @RequestBody RuleRevisionRequest request
    ) {
        return ApiResponse.ok(governanceService.reviseDraft(
                draftId, request.rule(), operatorId(request), changeReason(request)));
    }

    @PostMapping("/rules/{ruleId}/disable")
    public ApiResponse<PromotionRuleVersion> disableRule(
            @PathVariable String ruleId,
            @RequestBody(required = false) RuleActionRequest request
    ) {
        return ApiResponse.ok(governanceService.disableRule(ruleId, operatorId(request), changeReason(request)));
    }

    private String operatorId(RuleActionRequest request) {
        return request == null || request.operatorId() == null || request.operatorId().isBlank()
                ? "system"
                : request.operatorId();
    }

    private String changeReason(RuleActionRequest request) {
        return request == null || request.changeReason() == null ? "" : request.changeReason();
    }

    private String operatorId(RuleRevisionRequest request) {
        return request == null || request.operatorId() == null || request.operatorId().isBlank()
                ? "system"
                : request.operatorId();
    }

    private String changeReason(RuleRevisionRequest request) {
        return request == null || request.changeReason() == null ? "" : request.changeReason();
    }

    public record RuleActionRequest(String operatorId, String changeReason) {
    }

    public record RuleRevisionRequest(
            PromotionRule rule,
            String operatorId,
            String changeReason
    ) {
    }
}
