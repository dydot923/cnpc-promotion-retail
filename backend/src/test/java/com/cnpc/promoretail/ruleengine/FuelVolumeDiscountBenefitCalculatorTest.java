package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.FuelVolumeDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.explanation.DefaultExplanationBuilder;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.DateCondition;
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

class FuelVolumeDiscountBenefitCalculatorTest {

    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            List.<BenefitCalculator>of(new FuelVolumeDiscountBenefitCalculator()),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void cn98VolumeDiscountCalculatesByLiter() {
        CalculationResult result = engine.calculate(order(FuelType.CN98, "28.5", LocalDate.of(2026, 7, 8)),
                List.of(rule()));

        assertThat(result.discountAmount()).isEqualByComparingTo("22.80");
        assertThat(result.payableAmount()).isEqualByComparingTo("77.20");
        assertThat(result.discountAmount().scale()).isEqualTo(2);
        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-cn98-volume");
    }

    @Test
    void insufficientVolumeBlocksRule() {
        CalculationResult result = engine.calculate(order(FuelType.CN98, "0.5", LocalDate.of(2026, 7, 8)),
                List.of(rule()));

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品升数未满1升"));
    }

    @Test
    void fuelTypeMismatchBlocksRule() {
        CalculationResult result = engine.calculate(order(FuelType.GASOLINE, "28.5", LocalDate.of(2026, 7, 8)),
                List.of(rule()));

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("油品类型"));
    }

    @Test
    void nonActivityDateBlocksRule() {
        CalculationResult result = engine.calculate(order(FuelType.CN98, "28.5", LocalDate.of(2026, 7, 9)),
                List.of(rule()));

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("非活动日期"));
    }

    private static PromotionRule rule() {
        PromotionCondition condition = new PromotionCondition(Set.of(), Set.of(), Set.of(FuelType.CN98), Set.of(),
                Set.of(), null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                DateCondition.monthlyDates(Set.of(8, 18, 28)), null, Set.of(), Set.of(), false,
                new BigDecimal("1.00"));
        return new PromotionRule("cn98-volume", "CN98按升立减", PromotionRuleType.FUEL_VOLUME_DISCOUNT,
                50, "direct_discount", false, PromotionRuleStatus.CONFIRMED, condition,
                PromotionBenefit.fuelVolumeDiscount(new BigDecimal("0.80")), "test-v1");
    }

    private static OrderContext order(FuelType fuelType, String volume, LocalDate date) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                new CustomerContext(true, "gold", List.of(), 7),
                new FuelContext(fuelType, "CN98", new BigDecimal("100.00"), new BigDecimal(volume)),
                List.of(),
                date,
                LocalTime.of(10, 0)
        );
    }
}
