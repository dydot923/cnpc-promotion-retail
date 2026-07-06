package com.cnpc.promoretail.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.model.PromotionRuleDraft;
import com.cnpc.promoretail.promotion.model.PromotionRuleVersion;
import com.cnpc.promoretail.checkout.repository.InMemoryCheckoutCalculationRecordRepository;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.AmountOffBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.BundlePriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.ExchangePurchaseBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FixedPriceBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftCouponBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.GiftItemBenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CheckoutApplicationServiceTest {

    private final InMemoryPromotionRuleRepository repository = new InMemoryPromotionRuleRepository();
    private final InMemoryCheckoutCalculationRecordRepository calculationRecordRepository =
            new InMemoryCheckoutCalculationRecordRepository();
    private final PromotionRuleGovernanceService governanceService = new PromotionRuleGovernanceService(repository);
    private final CheckoutApplicationService checkoutApplicationService =
            new CheckoutApplicationService(engine(), repository, calculationRecordRepository);

    @Test
    void calculateFallsBackToOriginalPriceWhenNoRuleIsConfirmed() {
        governanceService.createDraft(importedRule(), "importer");

        CalculationResult result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        assertThat(result.recommendedCandidateId()).isEqualTo("original-price");
        assertThat(result.payableAmount()).isEqualByComparingTo("12.00");
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.ruleVersionIds()).isEmpty();
        assertThat(calculationRecordRepository.findAll()).hasSize(1);
        assertThat(calculationRecordRepository.findAll().getFirst().resultSnapshot().recommendedCandidateId())
                .isEqualTo("original-price");
    }

    @Test
    void calculateLoadsOnlyConfirmedRulesAndReturnsRuleVersionIds() {
        PromotionRuleDraft draft = governanceService.createDraft(importedRule(), "importer");
        PromotionRuleVersion version = governanceService.confirmDraft(draft.draftId(), "manager", "确认执行");

        CalculationResult result = checkoutApplicationService.calculate(new CheckoutCalculateRequest(order()));

        assertThat(result.recommendedCandidateId()).isEqualTo("cand-import-fixed-9_9-70424725");
        assertThat(result.payableAmount()).isEqualByComparingTo("9.90");
        assertThat(result.discountAmount()).isEqualByComparingTo("2.10");
        assertThat(result.ruleVersion()).isEqualTo(version.versionId());
        assertThat(result.ruleVersionIds()).containsExactly(version.versionId());
        assertThat(calculationRecordRepository.findAll()).hasSize(1);
        assertThat(calculationRecordRepository.findAll().getFirst().ruleVersionIds()).containsExactly(version.versionId());
        assertThat(calculationRecordRepository.findAll().getFirst().requestSnapshot().cartItems().getFirst().productCode())
                .isEqualTo("70424725");
    }

    private ImportedPromotionRule importedRule() {
        return new ImportedPromotionRule(new ImportVersion("import-v1"), "参考2-9.9元商品专区", 4,
                new PromotionRule("import-fixed-9_9-70424725", "9.9元专区-奥利奥",
                        PromotionRuleType.FIXED_PRICE, 50, "direct_discount", false,
                        PromotionRuleStatus.PENDING_CONFIRMATION,
                        new PromotionCondition(Set.of("70424725"), Set.of(), Set.of(), Set.of(), Set.of(),
                                null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ONE),
                        PromotionBenefit.fixedPrice(new BigDecimal("9.90")),
                        "import-v1"));
    }

    private OrderContext order() {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                new CustomerContext(true, "gold", List.of()),
                FuelContext.empty(),
                List.of(new CartItem("line-1", "70424725", "barcode-70424725",
                        "奥利奥 0糖夹心饼干 97g", 1, new BigDecimal("12.00"), "零食", new BigDecimal("20"))),
                LocalDate.of(2026, 7, 9),
                LocalTime.of(20, 30)
        );
    }

    private PromotionEngine engine() {
        return new DefaultPromotionEngine(
                new DefaultConditionMatcher(),
                calculators(),
                new DefaultConflictResolver(),
                new DefaultCandidateRanker(),
                new DefaultExplanationBuilder()
        );
    }

    private List<BenefitCalculator> calculators() {
        return List.of(
                new FixedPriceBenefitCalculator(),
                new PercentageDiscountBenefitCalculator(),
                new AmountOffBenefitCalculator(),
                new ExchangePurchaseBenefitCalculator(),
                new GiftItemBenefitCalculator(),
                new GiftCouponBenefitCalculator(),
                new BundlePriceBenefitCalculator()
        );
    }
}
