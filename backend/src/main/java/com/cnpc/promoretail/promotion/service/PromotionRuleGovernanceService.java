package com.cnpc.promoretail.promotion.service;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.promotion.PromotionRuleReviseRequest;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditAction;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PromotionRuleGovernanceService {

    private final PromotionRuleRepository promotionRuleRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public PromotionRuleGovernanceService(
            PromotionRuleRepository promotionRuleRepository,
            AuditLogService auditLogService
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.auditLogService = auditLogService;
    }

    public PromotionRuleGovernanceService(PromotionRuleRepository promotionRuleRepository) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.auditLogService = AuditLogService.noop();
    }

    public PromotionRuleDraft createDraft(ImportedPromotionRule importedRule, String operatorId) {
        PromotionRule pendingRule = importedRule.rule().withStatus(PromotionRuleStatus.PENDING_CONFIRMATION);
        PromotionRuleDraft existingDraft = promotionRuleRepository.findDraftByRuleId(pendingRule.ruleId()).orElse(null);
        PromotionRuleDraft draft = new PromotionRuleDraft(
                "draft-" + pendingRule.ruleId(),
                pendingRule,
                importedRule.importId().value(),
                importedRule.sourceSheetName(),
                importedRule.sourceRowNumber(),
                PromotionRuleStatus.PENDING_CONFIRMATION,
                false,
                Instant.now(),
                Instant.now(),
                operatorId
        );
        PromotionRuleDraft savedDraft = promotionRuleRepository.saveDraft(draft);
        if (existingDraft == null || savedDraft.sourceImportId().equals(draft.sourceImportId())) {
            promotionRuleRepository.appendAuditLog(audit(savedDraft.rule().ruleId(), PromotionRuleAuditAction.IMPORTED,
                    null, savedDraft.status(), operatorId, "Excel import candidate rule"));
            recordAudit("PROMOTION_RULE_IMPORT", savedDraft.rule().ruleId(), existingDraft, savedDraft,
                    operatorId, "Excel import candidate rule");
        }
        return savedDraft;
    }

    public PromotionRuleVersion confirmDraft(String draftId, String operatorId, String changeReason) {
        PromotionRuleDraft draft = requiredDraft(draftId);
        String versionId = "rule-version-" + UUID.randomUUID();
        PromotionRule confirmedRule = draft.rule()
                .withStatus(PromotionRuleStatus.CONFIRMED)
                .withVersion(versionId);
        PromotionRuleDraft confirmedDraft = draft.withRule(confirmedRule, PromotionRuleStatus.CONFIRMED, true);
        promotionRuleRepository.saveDraft(confirmedDraft, true);

        PromotionRuleVersion version = new PromotionRuleVersion(
                versionId,
                confirmedRule.ruleId(),
                draft.sourceImportId(),
                draft.sourceSheetName(),
                draft.sourceRowNumber(),
                confirmedRule.ruleType(),
                PromotionRuleStatus.CONFIRMED,
                Instant.now(),
                operatorId,
                Instant.now(),
                operatorId,
                changeReason,
                confirmedRule
        );
        promotionRuleRepository.saveVersion(version);
        promotionRuleRepository.appendAuditLog(audit(confirmedRule.ruleId(), PromotionRuleAuditAction.CONFIRMED,
                draft.status(), PromotionRuleStatus.CONFIRMED, operatorId, changeReason));
        recordAudit("PROMOTION_RULE_CONFIRM", confirmedRule.ruleId(), draft, confirmedDraft, operatorId, changeReason);
        return version;
    }

    public PromotionRuleDraft rejectDraft(String draftId, String operatorId, String changeReason) {
        PromotionRuleDraft draft = requiredDraft(draftId);
        PromotionRuleDraft rejectedDraft = draft.withStatus(PromotionRuleStatus.REJECTED, true);
        promotionRuleRepository.saveDraft(rejectedDraft, true);
        promotionRuleRepository.appendAuditLog(audit(rejectedDraft.rule().ruleId(), PromotionRuleAuditAction.REJECTED,
                draft.status(), PromotionRuleStatus.REJECTED, operatorId, changeReason));
        recordAudit("PROMOTION_RULE_REJECT", rejectedDraft.rule().ruleId(), draft, rejectedDraft, operatorId,
                changeReason);
        return rejectedDraft;
    }

    public PromotionRuleDraft reviseDraft(
            String draftId,
            PromotionRule revisedRule,
            String operatorId,
            String changeReason
    ) {
        PromotionRuleDraft draft = requiredDraft(draftId);
        PromotionRule pendingRule = revisedRule.withStatus(PromotionRuleStatus.PENDING_CONFIRMATION);
        PromotionRuleDraft revisedDraft = draft.withRule(pendingRule, PromotionRuleStatus.PENDING_CONFIRMATION, true);
        promotionRuleRepository.saveDraft(revisedDraft, true);
        promotionRuleRepository.appendAuditLog(audit(revisedDraft.rule().ruleId(), PromotionRuleAuditAction.REVISED,
                draft.status(), PromotionRuleStatus.PENDING_CONFIRMATION, operatorId, changeReason));
        recordAudit("PROMOTION_RULE_REVISE", revisedDraft.rule().ruleId(), draft, revisedDraft, operatorId,
                changeReason);
        return revisedDraft;
    }

    public PromotionRuleDraft reviseDraft(
            String draftId,
            PromotionRuleReviseRequest request
    ) {
        PromotionRuleDraft draft = requiredDraft(draftId);
        if (!draft.rule().ruleId().equals(request.ruleId())) {
            throw new IllegalArgumentException("request ruleId does not match draft ruleId");
        }

        PromotionRule current = draft.rule();
        PromotionCondition condition = request.ruleParams() != null && request.ruleParams().condition() != null
                ? request.ruleParams().condition()
                : current.condition();
        if (request.startTime() != null || request.endTime() != null) {
            condition = new PromotionCondition(
                    condition.productCodes(),
                    condition.excludedCategories(),
                    condition.fuelTypes(),
                    condition.stationTypes(),
                    condition.daysOfMonth(),
                    request.startTime() == null ? condition.startDate() : request.startTime(),
                    request.endTime() == null ? condition.endDate() : request.endTime(),
                    condition.minCartAmount(),
                    condition.minFuelAmount(),
                    condition.memberRequired(),
                    condition.minInventoryQuantity()
            );
        }

        PromotionBenefit benefit = request.ruleParams() != null && request.ruleParams().benefit() != null
                ? request.ruleParams().benefit()
                : current.benefit();
        PromotionRule revisedRule = new PromotionRule(
                current.ruleId(),
                current.activityName(),
                current.ruleType(),
                request.priority() == null ? current.priority() : request.priority(),
                request.exclusiveGroup() == null ? current.exclusiveGroup() : request.exclusiveGroup(),
                request.stackable() == null ? current.stackable() : request.stackable(),
                PromotionRuleStatus.PENDING_CONFIRMATION,
                condition,
                benefit,
                current.version()
        );
        return reviseDraft(draftId, revisedRule, request.operatorId(), request.reason());
    }

    public PromotionRuleVersion disableRule(String ruleId, String operatorId, String changeReason) {
        PromotionRuleDraft draft = requiredDraftByRuleId(ruleId);
        if (draft.status() != PromotionRuleStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed rules can be disabled: " + ruleId);
        }

        String versionId = "rule-version-" + UUID.randomUUID();
        PromotionRule disabledRule = draft.rule()
                .withStatus(PromotionRuleStatus.DISABLED)
                .withVersion(versionId);
        PromotionRuleDraft disabledDraft = draft.withRule(disabledRule, PromotionRuleStatus.DISABLED, true);
        promotionRuleRepository.saveDraft(disabledDraft, true);

        PromotionRuleVersion version = new PromotionRuleVersion(
                versionId,
                disabledRule.ruleId(),
                draft.sourceImportId(),
                draft.sourceSheetName(),
                draft.sourceRowNumber(),
                disabledRule.ruleType(),
                PromotionRuleStatus.DISABLED,
                Instant.now(),
                operatorId,
                Instant.now(),
                operatorId,
                changeReason,
                disabledRule
        );
        promotionRuleRepository.saveVersion(version);
        promotionRuleRepository.appendAuditLog(audit(disabledRule.ruleId(), PromotionRuleAuditAction.DISABLED,
                draft.status(), PromotionRuleStatus.DISABLED, operatorId, changeReason));
        recordAudit("PROMOTION_RULE_DISABLE", disabledRule.ruleId(), draft, disabledDraft, operatorId, changeReason);
        return version;
    }

    public List<PromotionRuleVersion> batchConfirmRules(
            List<String> ruleIds,
            String operatorId,
            String changeReason
    ) {
        return normalizeRuleIds(ruleIds).stream()
                .map(ruleId -> {
                    PromotionRuleDraft before = requiredDraftByRuleId(ruleId);
                    PromotionRuleVersion version = confirmDraft(before.draftId(), operatorId, changeReason);
                    PromotionRuleDraft after = requiredDraftByRuleId(ruleId);
                    promotionRuleRepository.appendAuditLog(audit(ruleId, PromotionRuleAuditAction.BATCH_CONFIRMED,
                            before.status(), PromotionRuleStatus.CONFIRMED, operatorId, changeReason));
                    recordAudit("PROMOTION_RULE_BATCH_CONFIRM", ruleId, before, after, operatorId, changeReason);
                    return version;
                })
                .toList();
    }

    public List<PromotionRuleDraft> batchReviseRules(
            List<PromotionRuleReviseRequest> requests,
            String operatorId,
            String changeReason
    ) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(request -> {
                    PromotionRuleDraft before = requiredDraftByRuleId(request.ruleId());
                    PromotionRuleReviseRequest effectiveRequest = new PromotionRuleReviseRequest(
                            request.ruleId(),
                            request.priority(),
                            request.exclusiveGroup(),
                            request.stackable(),
                            request.startTime(),
                            request.endTime(),
                            request.ruleParams(),
                            changeReason == null || changeReason.isBlank() ? request.reason() : changeReason,
                            operatorId,
                            request.operatorName()
                    );
                    PromotionRuleDraft after = reviseDraft(before.draftId(), effectiveRequest);
                    promotionRuleRepository.appendAuditLog(audit(after.rule().ruleId(), PromotionRuleAuditAction.BATCH_REVISED,
                            before.status(), after.status(), operatorId, effectiveRequest.reason()));
                    recordAudit("PROMOTION_RULE_BATCH_REVISE", after.rule().ruleId(), before, after,
                            operatorId, effectiveRequest.reason());
                    return after;
                })
                .toList();
    }

    public List<PromotionRuleVersion> batchArchiveRules(
            List<String> ruleIds,
            String operatorId,
            String changeReason
    ) {
        return normalizeRuleIds(ruleIds).stream()
                .map(ruleId -> archiveRule(ruleId, operatorId, changeReason))
                .toList();
    }

    public PromotionRuleVersion archiveRule(String ruleId, String operatorId, String changeReason) {
        PromotionRuleDraft draft = requiredDraftByRuleId(ruleId);
        String versionId = "rule-version-" + UUID.randomUUID();
        PromotionRule archivedRule = draft.rule()
                .withStatus(PromotionRuleStatus.ARCHIVED)
                .withVersion(versionId);
        PromotionRuleDraft archivedDraft = draft.withRule(archivedRule, PromotionRuleStatus.ARCHIVED, true);
        promotionRuleRepository.saveDraft(archivedDraft, true);

        PromotionRuleVersion version = new PromotionRuleVersion(
                versionId,
                archivedRule.ruleId(),
                draft.sourceImportId(),
                draft.sourceSheetName(),
                draft.sourceRowNumber(),
                archivedRule.ruleType(),
                PromotionRuleStatus.ARCHIVED,
                Instant.now(),
                operatorId,
                Instant.now(),
                operatorId,
                changeReason,
                archivedRule
        );
        promotionRuleRepository.saveVersion(version);
        promotionRuleRepository.appendAuditLog(audit(archivedRule.ruleId(), PromotionRuleAuditAction.BATCH_ARCHIVED,
                draft.status(), PromotionRuleStatus.ARCHIVED, operatorId, changeReason));
        recordAudit("PROMOTION_RULE_BATCH_ARCHIVE", archivedRule.ruleId(), draft, archivedDraft,
                operatorId, changeReason);
        return version;
    }

    public List<PromotionRuleDraft> listDrafts(PromotionRuleStatus status) {
        return promotionRuleRepository.findDraftsByStatus(status);
    }

    public List<PromotionRuleAuditLog> auditLogs(String ruleId) {
        return promotionRuleRepository.findAuditLogsByRuleId(ruleId);
    }

    private PromotionRuleDraft requiredDraft(String draftId) {
        return promotionRuleRepository.findDraftById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion rule draft does not exist: " + draftId));
    }

    private PromotionRuleDraft requiredDraftByRuleId(String ruleId) {
        return promotionRuleRepository.findDraftByRuleId(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion rule draft does not exist: " + ruleId));
    }

    private List<String> normalizeRuleIds(List<String> ruleIds) {
        if (ruleIds == null) {
            return List.of();
        }
        return ruleIds.stream()
                .filter(ruleId -> ruleId != null && !ruleId.isBlank())
                .distinct()
                .toList();
    }

    private PromotionRuleAuditLog audit(
            String ruleId,
            PromotionRuleAuditAction action,
            PromotionRuleStatus before,
            PromotionRuleStatus after,
            String operatorId,
            String reason
    ) {
        return new PromotionRuleAuditLog("audit-" + UUID.randomUUID(), ruleId, action,
                before, after, operatorId, reason, Instant.now());
    }

    private void recordAudit(
            String actionType,
            String ruleId,
            Object beforeSnapshot,
            Object afterSnapshot,
            String operatorId,
            String reason
    ) {
        auditLogService.record(actionType, "PROMOTION_RULE", ruleId,
                beforeSnapshot, afterSnapshot, operatorId, "", reason);
    }
}
