package com.cnpc.promoretail.ruleengine.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.datetrigger.InMemoryPromotionDateTriggerRepository;
import com.cnpc.promoretail.ruleengine.datetrigger.PromotionDateTrigger;
import com.cnpc.promoretail.ruleengine.model.DateCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DateConditionMatcherTest {

    private final InMemoryPromotionDateTriggerRepository repository =
            new InMemoryPromotionDateTriggerRepository();
    private final DateConditionMatcher matcher = new DateConditionMatcher(repository);

    @Test
    void daysOfMonthTriggerMatched() {
        repository.save(trigger("test-rule", "MONTHLY_DATES", Set.of(7, 17, 27),
                null, null, null, null));

        List<String> reasons = matcher.mismatchReasons(order(LocalDate.of(2026, 7, 7), LocalTime.of(10, 0)),
                rule("test-rule"));

        assertThat(reasons).isEmpty();
    }

    @Test
    void daysOfMonthTriggerNotMatched() {
        repository.save(trigger("test-rule", "DAYS_OF_MONTH", Set.of(7, 17, 27),
                null, null, null, null));

        List<String> reasons = matcher.mismatchReasons(order(LocalDate.of(2026, 7, 8), LocalTime.of(10, 0)),
                rule("test-rule"));

        assertThat(reasons).containsExactly("日期触发器未命中规则：test-rule");
    }

    @Test
    void dateRangeTriggerMatchedAndNotMatched() {
        repository.save(trigger("event-rule", "DATE_RANGE", Set.of(),
                LocalDate.of(2026, 6, 12), LocalDate.of(2026, 8, 9), null, null));

        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 6, 15), LocalTime.NOON),
                rule("event-rule"))).isEmpty();
        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 8, 10), LocalTime.NOON),
                rule("event-rule"))).isNotEmpty();
    }

    @Test
    void excludeMonthlyDatesConditionBlocksConfiguredDates() {
        DateCondition condition = DateCondition.excludeMonthlyDates(Set.of(10, 20, 30));

        assertThat(matcher.matches(LocalDate.of(2026, 7, 11), condition)).isTrue();
        assertThat(matcher.matches(LocalDate.of(2026, 7, 10), condition)).isFalse();
    }

    @Test
    void dateTimeRangeTriggerSupportsCrossMidnight() {
        repository.save(trigger("night-rule", "DATE_TIME_RANGE", Set.of(),
                LocalDate.of(2026, 6, 12), LocalDate.of(2026, 8, 9),
                LocalTime.of(18, 0), LocalTime.of(2, 0)));

        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 7, 1), LocalTime.of(23, 0)),
                rule("night-rule"))).isEmpty();
        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 7, 1), LocalTime.of(1, 30)),
                rule("night-rule"))).isEmpty();
        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 7, 1), LocalTime.of(12, 0)),
                rule("night-rule"))).isNotEmpty();
    }

    @Test
    void multipleTriggersUseAnyMatchedLogic() {
        repository.save(trigger("multi-rule", "MONTHLY_DATES", Set.of(7), null, null, null, null));
        repository.save(trigger("multi-rule", "DATE_RANGE", Set.of(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, null));

        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 9, 15), LocalTime.NOON),
                rule("multi-rule"))).isEmpty();
    }

    @Test
    void noTriggersDoNotBlockRule() {
        assertThat(matcher.mismatchReasons(order(LocalDate.of(2026, 7, 11), LocalTime.NOON),
                rule("plain-rule"))).isEmpty();
    }

    @Test
    void defaultConditionMatcherUsesDateTriggerRepository() {
        repository.save(trigger("engine-rule", "MONTHLY_DATES", Set.of(10, 20, 30),
                null, null, null, null));
        DefaultConditionMatcher conditionMatcher = new DefaultConditionMatcher(repository);

        assertThat(conditionMatcher.match(order(LocalDate.of(2026, 7, 10), LocalTime.NOON),
                rule("engine-rule")).matched()).isTrue();
        assertThat(conditionMatcher.match(order(LocalDate.of(2026, 7, 11), LocalTime.NOON),
                rule("engine-rule")).matched()).isFalse();
    }

    private PromotionDateTrigger trigger(
            String ruleId,
            String triggerType,
            Set<Integer> daysOfMonth,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime timeFrom,
            LocalTime timeTo
    ) {
        return new PromotionDateTrigger(
                null,
                "activity",
                ruleId,
                triggerType,
                daysOfMonth,
                startDate,
                endDate,
                timeFrom,
                timeTo,
                "test",
                "test",
                1,
                true
        );
    }

    private PromotionRule rule(String ruleId) {
        return new PromotionRule(
                ruleId,
                ruleId,
                PromotionRuleType.PERCENTAGE_DISCOUNT,
                50,
                "direct_discount",
                false,
                PromotionRuleStatus.CONFIRMED,
                PromotionCondition.empty(),
                PromotionBenefit.percentageDiscount(new BigDecimal("0.90")),
                "test-v1"
        );
    }

    private OrderContext order(LocalDate date, LocalTime time) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "Xinjiang"),
                new CustomerContext(false, "", List.of()),
                FuelContext.empty(),
                List.of(new CartItem("line-1", "sku-1", "barcode", "Item", 1,
                        new BigDecimal("100.00"), "store", new BigDecimal("10"))),
                date,
                time
        );
    }
}
