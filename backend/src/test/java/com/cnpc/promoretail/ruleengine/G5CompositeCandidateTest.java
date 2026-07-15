package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.CompositeBenefitComponent;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class G5CompositeCandidateTest extends PostgresIntegrationTestSupport {

    private static final String RULE_ID = "abv2-g5-mid-autumn-composite";
    private static final StationContext STATION = new StationContext("station-001", "gas_station", "新疆");

    @Test
    void septemberMemberAtThresholdGetsOneCompositeCandidate() {
        CalculationResult result = calculate("226.00", LocalDate.of(2026, 9, 15));

        assertThat(result.availableCandidates()).filteredOn(candidate -> candidate.ruleId().equals(RULE_ID))
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.ruleType()).isEqualTo(PromotionRuleType.COMPOSITE);
                    assertThat(candidate.discountAmount()).isEqualByComparingTo("50.00");
                    assertThat(candidate.payableAmount()).isEqualByComparingTo("176.00");
                    assertThat(candidate.coupons()).singleElement().satisfies(coupon -> {
                        assertThat(coupon.amount()).isEqualByComparingTo("10.00");
                        assertThat(coupon.quantity()).isEqualTo(2);
                    });
                    assertThat(candidate.compositeComponents()).extracting(CompositeBenefitComponent::type)
                            .containsExactly(PromotionRuleType.AMOUNT_OFF, PromotionRuleType.GIFT_COUPON);
                    assertThat(candidate.explanation()).contains("满减50.00元", "2张10.00元");
                });
    }

    @Test
    void amountBelowThresholdReturnsBlockedReason() {
        CalculationResult result = calculate("225.00", LocalDate.of(2026, 9, 15));

        assertThat(result.availableCandidates()).noneMatch(candidate -> candidate.ruleId().equals(RULE_ID));
        assertThat(blockedReasons(result)).anyMatch(reason -> reason.contains("购物车金额未达到"));
    }

    @Test
    void dateOutsideSeptemberReturnsBlockedReason() {
        CalculationResult result = calculate("226.00", LocalDate.of(2026, 8, 31));

        assertThat(result.availableCandidates()).noneMatch(candidate -> candidate.ruleId().equals(RULE_ID));
        assertThat(blockedReasons(result)).anyMatch(reason -> reason.contains("早于活动开始日期"));
    }

    @Test
    void nonMemberReturnsBlockedReason() {
        CalculationResult result = calculate("226.00", LocalDate.of(2026, 9, 15),
                CustomerContext.anonymous(), "月饼礼盒");

        assertThat(result.availableCandidates()).noneMatch(candidate -> candidate.ruleId().equals(RULE_ID));
        assertThat(blockedReasons(result)).anyMatch(reason -> reason.contains("不是会员"));
    }

    @Test
    void wrongCategoryReturnsBlockedReason() {
        CalculationResult result = calculate("226.00", LocalDate.of(2026, 9, 15),
                new CustomerContext(true, "GOLD", List.of()), "普通食品");

        assertThat(result.availableCandidates()).noneMatch(candidate -> candidate.ruleId().equals(RULE_ID));
        assertThat(blockedReasons(result)).anyMatch(reason -> reason.contains("品类不在活动范围"));
    }

    private CalculationResult calculate(String amount, LocalDate date) {
        return calculate(amount, date, new CustomerContext(true, "GOLD", List.of()), "月饼礼盒");
    }

    private CalculationResult calculate(
            String amount,
            LocalDate date,
            CustomerContext customer,
            String category
    ) {
        return calculate(order(
                STATION,
                customer,
                FuelContext.empty(),
                List.of(syntheticItem("70538246", "moon cake", 1, amount, category)),
                date,
                LocalTime.of(10, 0),
                List.of()
        ));
    }

    private List<String> blockedReasons(CalculationResult result) {
        return result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleId().equals(RULE_ID))
                .map(BlockedPromotion::reasons)
                .flatMap(List::stream)
                .map(reason -> reason.message())
                .toList();
    }
}
