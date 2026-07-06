package com.cnpc.promoretail.promotion.repository;

import com.cnpc.promoretail.promotion.model.PromotionRuleAuditLog;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPromotionRuleRepository implements PromotionRuleRepository {

    private final ConcurrentMap<String, PromotionRuleDraft> draftsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> draftIdByRuleId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PromotionRuleVersion> confirmedVersionByRuleId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<PromotionRuleAuditLog>> auditLogsByRuleId = new ConcurrentHashMap<>();

    @Override
    public PromotionRuleDraft saveDraft(PromotionRuleDraft draft) {
        return saveDraft(draft, false);
    }

    @Override
    public PromotionRuleDraft saveDraft(PromotionRuleDraft draft, boolean overwriteManualLocked) {
        PromotionRuleDraft existing = findDraftByRuleId(draft.rule().ruleId()).orElse(null);
        if (!overwriteManualLocked && existing != null && existing.manualLocked()) {
            return existing;
        }
        draftsById.put(draft.draftId(), draft);
        draftIdByRuleId.put(draft.rule().ruleId(), draft.draftId());
        return draft;
    }

    @Override
    public Optional<PromotionRuleDraft> findDraftById(String draftId) {
        return Optional.ofNullable(draftsById.get(draftId));
    }

    @Override
    public Optional<PromotionRuleDraft> findDraftByRuleId(String ruleId) {
        String draftId = draftIdByRuleId.get(ruleId);
        return draftId == null ? Optional.empty() : findDraftById(draftId);
    }

    @Override
    public PromotionRuleVersion saveVersion(PromotionRuleVersion version) {
        if (version.status() == PromotionRuleStatus.CONFIRMED) {
            confirmedVersionByRuleId.put(version.ruleId(), version);
        } else if (version.status() == PromotionRuleStatus.DISABLED) {
            confirmedVersionByRuleId.remove(version.ruleId());
        }
        return version;
    }

    @Override
    public List<PromotionRule> findConfirmedRules() {
        return draftsById.values().stream()
                .filter(draft -> draft.status() == PromotionRuleStatus.CONFIRMED)
                .map(PromotionRuleDraft::rule)
                .filter(PromotionRule::active)
                .sorted(Comparator.comparing(PromotionRule::ruleId))
                .toList();
    }

    @Override
    public void appendAuditLog(PromotionRuleAuditLog auditLog) {
        auditLogsByRuleId.compute(auditLog.ruleId(), (ruleId, logs) -> {
            List<PromotionRuleAuditLog> mutable = logs == null ? new ArrayList<>() : new ArrayList<>(logs);
            mutable.add(auditLog);
            return List.copyOf(mutable);
        });
    }

    @Override
    public List<PromotionRuleAuditLog> findAuditLogsByRuleId(String ruleId) {
        return auditLogsByRuleId.getOrDefault(ruleId, List.of());
    }
}
