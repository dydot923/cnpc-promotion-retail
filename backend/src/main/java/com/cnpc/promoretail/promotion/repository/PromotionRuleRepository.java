package com.cnpc.promoretail.promotion.repository;

import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.util.List;
import java.util.Optional;

public interface PromotionRuleRepository {

    PromotionRuleDraft saveDraft(PromotionRuleDraft draft);

    PromotionRuleDraft saveDraft(PromotionRuleDraft draft, boolean overwriteManualLocked);

    Optional<PromotionRuleDraft> findDraftById(String draftId);

    Optional<PromotionRuleDraft> findDraftByRuleId(String ruleId);

    PromotionRuleVersion saveVersion(PromotionRuleVersion version);

    List<PromotionRule> findConfirmedRules();

    void appendAuditLog(PromotionRuleAuditLog auditLog);

    List<PromotionRuleAuditLog> findAuditLogsByRuleId(String ruleId);
}
