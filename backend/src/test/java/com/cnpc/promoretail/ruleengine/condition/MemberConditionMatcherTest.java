package com.cnpc.promoretail.ruleengine.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MemberConditionMatcherTest {

    private final MemberConditionMatcher matcher = new MemberConditionMatcher();

    @Test
    void goldMemberMatchesGoldRule() {
        assertThat(matcher.mismatchReasons(
                order(customer("gold", 7, List.of())),
                condition(Set.of("gold"), false, Set.of())
        )).isEmpty();
    }

    @Test
    void platinumMemberMatchesSingleGoldMinimumRule() {
        assertThat(matcher.mismatchReasons(
                order(customer("platinum", 7, List.of())),
                condition(Set.of("gold"), false, Set.of())
        )).isEmpty();
    }

    @Test
    void silverMemberDoesNotMatchGoldRule() {
        assertThat(matcher.mismatchReasons(
                order(customer("silver", 7, List.of())),
                condition(Set.of("gold"), false, Set.of())
        )).contains("会员等级不满足活动要求");
    }

    @Test
    void memberWithoutLevelConditionPasses() {
        assertThat(matcher.mismatchReasons(
                order(customer("normal", 7, List.of())),
                condition(Set.of(), false, Set.of())
        )).isEmpty();
    }

    @Test
    void birthdayMonthRuleMatchesCurrentMonth() {
        assertThat(matcher.mismatchReasons(
                order(customer("normal", 7, List.of())),
                condition(Set.of(), true, Set.of())
        )).isEmpty();
    }

    @Test
    void birthdayMonthRuleBlocksOtherMonth() {
        assertThat(matcher.mismatchReasons(
                order(customer("normal", 8, List.of())),
                condition(Set.of(), true, Set.of())
        )).contains("当前月份不是会员生日月");
    }

    @Test
    void memberTagRuleMatchesAnyRequiredTag() {
        assertThat(matcher.mismatchReasons(
                order(customer("gold", 7, List.of("gasoline_customer", "high_value"))),
                condition(Set.of(), false, Set.of("gasoline_customer", "diesel_customer"))
        )).isEmpty();
    }

    @Test
    void memberTagRuleBlocksMissingTag() {
        assertThat(matcher.mismatchReasons(
                order(customer("gold", 7, List.of("diesel_customer"))),
                condition(Set.of(), false, Set.of("gasoline_customer"))
        )).contains("会员标签不满足活动要求");
    }

    private CustomerContext customer(String level, Integer birthMonth, List<String> tags) {
        return new CustomerContext(true, level, List.of(), birthMonth, "", "member-test", tags, null);
    }

    private OrderContext order(CustomerContext customer) {
        return new OrderContext(
                new StationContext("station-001", "gas_station", "新疆"),
                customer,
                FuelContext.empty(),
                List.of(new CartItem("line-1", "sku-1", "barcode-1", "商品", 1,
                        new BigDecimal("100.00"), "便利店", BigDecimal.TEN)),
                LocalDate.of(2026, 7, 14),
                LocalTime.of(10, 0)
        );
    }

    private PromotionCondition condition(Set<String> memberLevels, boolean birthdayMonthRequired,
                                         Set<String> memberTags) {
        return new PromotionCondition(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO,
                null, null, Set.of(), memberLevels, birthdayMonthRequired, memberTags,
                BigDecimal.ZERO, Set.of(), 0);
    }
}
