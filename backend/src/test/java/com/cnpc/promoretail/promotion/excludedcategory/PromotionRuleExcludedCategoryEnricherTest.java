package com.cnpc.promoretail.promotion.excludedcategory;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.DefaultPromotionEngine;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
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
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
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

class PromotionRuleExcludedCategoryEnricherTest {

    private final InMemoryPromotionExcludedCategoryRepository repository =
            new InMemoryPromotionExcludedCategoryRepository();
    private final PromotionRuleExcludedCategoryEnricher enricher =
            new PromotionRuleExcludedCategoryEnricher(repository);
    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            List.<BenefitCalculator>of(new PercentageDiscountBenefitCalculator()),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void structuredExcludedCategoriesAreMergedBeforeDiscountCalculation() {
        repository.save(new PromotionExcludedCategory("day9-store", "香烟", "活动看板明确排除品类"));
        PromotionRule enriched = enricher.enrich(discountRule("day9-store"));

        CalculationResult result = engine.calculate(order(List.of(
                item("snack", "零食", "100.00"),
                item("cigarette", "香烟", "100.00")
        )), List.of(enriched));

        PromotionCandidate candidate = candidate(result, "day9-store");
        assertThat(enriched.condition().excludedCategories()).containsExactlyInAnyOrder("香烟");
        assertThat(candidate.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(candidate.payableAmount()).isEqualByComparingTo("190.00");
    }

    @Test
    void structuredExcludedCategoriesBlockRuleWhenAllItemsAreExcluded() {
        repository.save(new PromotionExcludedCategory("day9-store", "香烟", "活动看板明确排除品类"));
        PromotionRule enriched = enricher.enrich(discountRule("day9-store"));

        CalculationResult result = engine.calculate(order(List.of(item("cigarette", "香烟", "100.00"))),
                List.of(enriched));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .doesNotContain("cand-day9-store");
        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("排除品类"));
    }

    private PromotionRule discountRule(String ruleId) {
        return new PromotionRule(ruleId, "逢9便利店9折", PromotionRuleType.PERCENTAGE_DISCOUNT, 50,
                "direct_discount", false, PromotionRuleStatus.CONFIRMED, PromotionCondition.empty(),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")), "test-v1");
    }

    private OrderContext order(List<CartItem> items) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                new CustomerContext(true, "gold", List.of()),
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                LocalTime.of(10, 0)
        );
    }

    private CartItem item(String productCode, String category, String price) {
        return new CartItem("line-" + productCode, productCode, null, productCode, 1,
                new BigDecimal(price), category, BigDecimal.TEN);
    }

    private PromotionCandidate candidate(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow();
    }
}
