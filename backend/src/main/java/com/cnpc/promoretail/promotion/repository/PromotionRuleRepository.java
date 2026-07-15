package com.cnpc.promoretail.promotion.repository;

import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.util.List;
import java.util.Optional;

public interface PromotionRuleRepository {

    PromotionRuleDraft saveDraft(PromotionRuleDraft draft);

    PromotionRuleDraft saveDraft(PromotionRuleDraft draft, boolean overwriteManualLocked);

    Optional<PromotionRuleDraft> findDraftById(String draftId);

    Optional<PromotionRuleDraft> findDraftByRuleId(String ruleId);

    List<PromotionRuleDraft> findDraftsByStatus(PromotionRuleStatus status);

    PromotionRuleVersion saveVersion(PromotionRuleVersion version);

    List<PromotionRule> findConfirmedRules();

    default boolean checkoutEligible(PromotionRule rule) {
        return rule != null && rule.active();
    }

    void appendAuditLog(PromotionRuleAuditLog auditLog);

    List<PromotionRuleAuditLog> findAuditLogsByRuleId(String ruleId);
}
