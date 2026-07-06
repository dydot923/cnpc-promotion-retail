package com.cnpc.promoretail.promotion.service;

import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditAction;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PromotionRuleGovernanceService {

    private final PromotionRuleRepository promotionRuleRepository;

    public PromotionRuleGovernanceService(PromotionRuleRepository promotionRuleRepository) {
        this.promotionRuleRepository = promotionRuleRepository;
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
                    null, savedDraft.status(), operatorId, "Excel导入候选规则"));
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
        return version;
    }

    public PromotionRuleDraft rejectDraft(String draftId, String operatorId, String changeReason) {
        PromotionRuleDraft draft = requiredDraft(draftId);
        PromotionRuleDraft rejectedDraft = draft.withStatus(PromotionRuleStatus.REJECTED, true);
        promotionRuleRepository.saveDraft(rejectedDraft, true);
        promotionRuleRepository.appendAuditLog(audit(rejectedDraft.rule().ruleId(), PromotionRuleAuditAction.REJECTED,
                draft.status(), PromotionRuleStatus.REJECTED, operatorId, changeReason));
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
        return revisedDraft;
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
        return version;
    }

    public List<PromotionRuleAuditLog> auditLogs(String ruleId) {
        return promotionRuleRepository.findAuditLogsByRuleId(ruleId);
    }

    private PromotionRuleDraft requiredDraft(String draftId) {
        return promotionRuleRepository.findDraftById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("规则草稿不存在: " + draftId));
    }

    private PromotionRuleDraft requiredDraftByRuleId(String ruleId) {
        return promotionRuleRepository.findDraftByRuleId(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion rule draft does not exist: " + ruleId));
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
}
