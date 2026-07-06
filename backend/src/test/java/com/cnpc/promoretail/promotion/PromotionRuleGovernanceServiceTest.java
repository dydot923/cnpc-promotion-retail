package com.cnpc.promoretail.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.importcenter.model.ImportType;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.model.PromotionRuleAuditAction;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromotionRuleGovernanceServiceTest {

    private final InMemoryPromotionRuleRepository repository = new InMemoryPromotionRuleRepository();
    private final PromotionRuleGovernanceService governanceService = new PromotionRuleGovernanceService(repository);

    @Test
    void importedRuleCreatesPendingDraftAndDoesNotEnterConfirmedRepository() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule("import-v1", "9.90"), "importer");

        assertThat(draft.status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
        assertThat(draft.rule().status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
        assertThat(repository.findConfirmedRules()).isEmpty();
        assertThat(governanceService.auditLogs(draft.rule().ruleId()))
                .extracting(log -> log.action())
                .containsExactly(PromotionRuleAuditAction.IMPORTED);
    }

    @Test
    void confirmedDraftCreatesVersionAndConfirmedRuleCanBeLoaded() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule("import-v1", "9.90"), "importer");

        PromotionRuleVersion version = governanceService.confirmDraft(draft.draftId(), "manager", "确认9.9专区规则");

        assertThat(version.status()).isEqualTo(PromotionRuleStatus.CONFIRMED);
        assertThat(version.versionId()).startsWith("rule-version-");
        assertThat(version.rule().active()).isTrue();
        assertThat(repository.findConfirmedRules()).hasSize(1);
        assertThat(repository.findConfirmedRules().getFirst().version()).isEqualTo(version.versionId());
        assertThat(governanceService.auditLogs(draft.rule().ruleId()))
                .extracting(log -> log.action())
                .containsExactly(PromotionRuleAuditAction.IMPORTED, PromotionRuleAuditAction.CONFIRMED);
    }

    @Test
    void disabledConfirmedRuleNoLongerParticipatesInCheckoutLoading() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule("import-v1", "9.90"), "importer");
        governanceService.confirmDraft(draft.draftId(), "manager", "confirm");

        PromotionRuleVersion disabledVersion =
                governanceService.disableRule(draft.rule().ruleId(), "manager", "disable after review");

        assertThat(disabledVersion.status()).isEqualTo(PromotionRuleStatus.DISABLED);
        assertThat(disabledVersion.rule().active()).isFalse();
        assertThat(repository.findConfirmedRules()).isEmpty();
        assertThat(repository.findDraftByRuleId(draft.rule().ruleId()))
                .hasValueSatisfying(disabledDraft ->
                        assertThat(disabledDraft.status()).isEqualTo(PromotionRuleStatus.DISABLED));
        assertThat(governanceService.auditLogs(draft.rule().ruleId()))
                .extracting(log -> log.action())
                .containsExactly(PromotionRuleAuditAction.IMPORTED, PromotionRuleAuditAction.CONFIRMED,
                        PromotionRuleAuditAction.DISABLED);
    }

    @Test
    void rejectedAndRevisedDraftsKeepAuditTrail() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule("import-v1", "9.90"), "importer");

        PromotionRuleDraft rejected = governanceService.rejectDraft(draft.draftId(), "manager", "商品编码待核对");
        PromotionRule revisedRule = rule("10.90").withVersion("manual-revision-v1");
        PromotionRuleDraft revised = governanceService.reviseDraft(rejected.draftId(), revisedRule, "manager", "修正促销价");

        assertThat(rejected.status()).isEqualTo(PromotionRuleStatus.REJECTED);
        assertThat(revised.status()).isEqualTo(PromotionRuleStatus.PENDING_CONFIRMATION);
        assertThat(revised.manualLocked()).isTrue();
        assertThat(revised.rule().benefit().fixedPrice()).isEqualByComparingTo("10.90");
        assertThat(governanceService.auditLogs(draft.rule().ruleId()))
                .extracting(log -> log.action())
                .containsExactly(PromotionRuleAuditAction.IMPORTED, PromotionRuleAuditAction.REJECTED,
                        PromotionRuleAuditAction.REVISED);
    }

    @Test
    void manuallyLockedDraftIsNotSilentlyOverwrittenByNextImport() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule("import-v1", "9.90"), "importer");
        PromotionRuleDraft rejected = governanceService.rejectDraft(draft.draftId(), "manager", "暂不执行");

        PromotionRuleDraft secondImport = governanceService.createDraft(importedRule("import-v2", "8.90"), "importer");

        assertThat(secondImport).isEqualTo(rejected);
        assertThat(secondImport.rule().benefit().fixedPrice()).isEqualByComparingTo("9.90");
        assertThat(secondImport.sourceImportId()).isEqualTo("import-v1");
        assertThat(governanceService.auditLogs(draft.rule().ruleId()))
                .extracting(log -> log.action())
                .containsExactly(PromotionRuleAuditAction.IMPORTED, PromotionRuleAuditAction.REJECTED);
    }

    private ImportedPromotionRule importedRule(String importId, String fixedPrice) {
        return new ImportedPromotionRule(new ImportVersion(importId), "参考2-9.9元商品专区", 4, rule(fixedPrice));
    }

    private PromotionRule rule(String fixedPrice) {
        return new PromotionRule("import-fixed-9_9-70424725", "9.9元专区-奥利奥",
                PromotionRuleType.FIXED_PRICE, 50, "direct_discount", false,
                PromotionRuleStatus.PENDING_CONFIRMATION,
                new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(), null, null,
                        BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ONE),
                PromotionBenefit.fixedPrice(new BigDecimal(fixedPrice)),
                ImportVersion.newVersion(ImportType.PROMOTION).value());
    }
}
