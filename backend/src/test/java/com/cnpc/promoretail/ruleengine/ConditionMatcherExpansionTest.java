package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.benefit.BenefitCalculator;
import com.cnpc.promoretail.ruleengine.benefit.PercentageDiscountBenefitCalculator;
import com.cnpc.promoretail.ruleengine.condition.DateConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.DefaultConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.MemberConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.ProvinceConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.StationTypeConditionMatcher;
import com.cnpc.promoretail.ruleengine.condition.TimeRangeConditionMatcher;
import com.cnpc.promoretail.ruleengine.conflict.DefaultConflictResolver;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
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
import com.cnpc.promoretail.ruleengine.model.TimeRangeCondition;
import com.cnpc.promoretail.ruleengine.ranking.DefaultCandidateRanker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConditionMatcherExpansionTest {

    private final PromotionEngine engine = new DefaultPromotionEngine(
            new DefaultConditionMatcher(),
            List.<BenefitCalculator>of(new PercentageDiscountBenefitCalculator()),
            new DefaultConflictResolver(),
            new DefaultCandidateRanker(),
            new DefaultExplanationBuilder()
    );

    @Test
    void dateConditionMatcherSupportsMonthlyDatesAndRanges() {
        DateConditionMatcher matcher = new DateConditionMatcher();

        assertThat(matcher.matches(LocalDate.of(2026, 7, 17), DateCondition.monthlyDates(Set.of(7, 17, 27)))).isTrue();
        assertThat(matcher.matches(LocalDate.of(2026, 7, 10), DateCondition.monthlyRange(9, 11))).isTrue();
        assertThat(matcher.matches(LocalDate.of(2026, 8, 9),
                DateCondition.dateRange(LocalDate.of(2026, 6, 12), LocalDate.of(2026, 8, 9)))).isTrue();
        assertThat(matcher.matches(LocalDate.of(2026, 7, 10), DateCondition.monthlyDates(Set.of(7, 17, 27)))).isFalse();
    }

    @Test
    void timeRangeMatcherSupportsCrossMidnight() {
        TimeRangeConditionMatcher matcher = new TimeRangeConditionMatcher();
        TimeRangeCondition night = new TimeRangeCondition(LocalTime.of(20, 0), LocalTime.of(6, 0));

        assertThat(matcher.matches(LocalTime.of(23, 0), night)).isTrue();
        assertThat(matcher.matches(LocalTime.of(5, 30), night)).isTrue();
        assertThat(matcher.matches(LocalTime.of(12, 0), night)).isFalse();
    }

    @Test
    void stationProvinceAndMemberMatchersReturnReasons() {
        PromotionCondition condition = condition(DateCondition.monthlyDates(Set.of(7)), null,
                Set.of("gas_cng"), Set.of("新疆"), Set.of("gold"), true);
        OrderContext context = order(LocalDate.of(2026, 7, 7), LocalTime.of(10, 0),
                "gas_station", "甘肃", false, "silver", 5);

        assertThat(new StationTypeConditionMatcher().mismatchReasons(context, condition)).contains("站点类型不匹配");
        assertThat(new ProvinceConditionMatcher().mismatchReasons(context, condition)).contains("省区不在活动范围内");
        assertThat(new MemberConditionMatcher().mismatchReasons(context, condition))
                .contains("会员等级不满足活动要求", "当前顾客不是会员，不能参与会员生日活动");
    }

    @Test
    void dayNineDiscountCanBeMatchedByEnhancedDateCondition() {
        PromotionRule rule = discountRule("day-nine", condition(DateCondition.monthlyDates(Set.of(9, 19, 29)),
                null, Set.of("gas_station"), Set.of(), Set.of(), false));

        CalculationResult result = engine.calculate(order(LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                "gas_station", "新疆", true, "gold", 7), List.of(rule));

        assertThat(result.availableCandidates()).extracting(PromotionCandidate::candidateId)
                .contains("cand-day-nine");
    }

    @Test
    void dayNineDiscountIsBlockedOnOtherDates() {
        PromotionRule rule = discountRule("day-nine", condition(DateCondition.monthlyDates(Set.of(9, 19, 29)),
                null, Set.of("gas_station"), Set.of(), Set.of(), false));

        CalculationResult result = engine.calculate(order(LocalDate.of(2026, 7, 10), LocalTime.of(10, 0),
                "gas_station", "新疆", true, "gold", 7), List.of(rule));

        assertThat(result.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("非活动日期"));
    }

    @Test
    void cngStationDaySevenDiscountRequiresStationType() {
        PromotionRule rule = discountRule("cng-day-seven", condition(DateCondition.monthlyDates(Set.of(7, 17, 27)),
                null, Set.of("gas_cng"), Set.of(), Set.of(), false));

        CalculationResult hit = engine.calculate(order(LocalDate.of(2026, 7, 7), LocalTime.of(10, 0),
                "gas_cng", "新疆", true, "gold", 7), List.of(rule));
        CalculationResult blocked = engine.calculate(order(LocalDate.of(2026, 7, 7), LocalTime.of(10, 0),
                "gas_station", "新疆", true, "gold", 7), List.of(rule));

        assertThat(hit.availableCandidates()).extracting(PromotionCandidate::candidateId).contains("cand-cng-day-seven");
        assertThat(blocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("站点类型不匹配"));
    }

    @Test
    void memberBirthdayRuleRequiresMemberAndBirthMonth() {
        PromotionRule rule = discountRule("birthday", condition(null, null, Set.of(), Set.of(), Set.of("gold"), true));

        CalculationResult hit = engine.calculate(order(LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                "gas_station", "新疆", true, "gold", 7), List.of(rule));
        CalculationResult blocked = engine.calculate(order(LocalDate.of(2026, 7, 9), LocalTime.of(10, 0),
                "gas_station", "新疆", false, "gold", 7), List.of(rule));

        assertThat(hit.availableCandidates()).extracting(PromotionCandidate::candidateId).contains("cand-birthday");
        assertThat(blocked.blockedPromotions().getFirst().reasons())
                .anySatisfy(reason -> assertThat(reason.message()).contains("不是会员"));
    }

    private static PromotionRule discountRule(String id, PromotionCondition condition) {
        return new PromotionRule(id, id, PromotionRuleType.PERCENTAGE_DISCOUNT, 50,
                "direct_discount", false, PromotionRuleStatus.CONFIRMED, condition,
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")), "test-v1");
    }

    private static PromotionCondition condition(DateCondition dateCondition, TimeRangeCondition timeRange,
                                                Set<String> stationTypes, Set<String> provinces,
                                                Set<String> memberLevels, boolean birthdayMonthRequired) {
        return new PromotionCondition(Set.of(), Set.of(), Set.of(), stationTypes, Set.of(),
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                dateCondition, timeRange, provinces, memberLevels, birthdayMonthRequired, BigDecimal.ZERO);
    }

    private static OrderContext order(LocalDate date, LocalTime time, String stationType, String province,
                                      boolean member, String memberLevel, Integer birthMonth) {
        return new OrderContext(
                new StationContext("station-001", stationType, province),
                new CustomerContext(member, memberLevel, List.of(), birthMonth),
                FuelContext.empty(),
                List.of(new CartItem("line-1", "sku-1", "barcode-1", "商品", 1,
                        new BigDecimal("100.00"), "便利店", new BigDecimal("20"))),
                date,
                time
        );
    }
}
