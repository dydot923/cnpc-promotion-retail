package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityStatusConfirmationIntegrationTest extends PostgresIntegrationTestSupport {

    private static final StationContext GAS_STATION =
            new StationContext("station-001", "gas_station", "新疆");
    private static final StationContext GAS_FILLING_STATION =
            new StationContext("station-cng-001", "gas_filling_station", "新疆");

    @Test
    void a1TieredCouponIsConfirmedAndExecutable() {
        String ruleId = "abv2-a1-day7-gas-coupon";
        CalculationResult result = calculate(List.of(rule(ruleId)), GAS_FILLING_STATION, member(),
                new FuelContext(FuelType.LNG, null, new BigDecimal("1000.00"), BigDecimal.ZERO),
                List.of(), LocalDate.of(2026, 7, 7));

        assertThat(candidate(result, ruleId).coupons()).hasSize(2)
                .extracting(coupon -> coupon.couponName())
                .containsExactly("30元LNG券", "12元便利店商品券");
    }

    @Test
    void e1GasolineAndDieselRulesReturnBothCoupons() {
        List<PromotionRule> rules = List.of(rule("abv2-e1-gasoline-gift-coupons"),
                rule("abv2-e1-diesel-gift-coupons"));
        CalculationResult gasoline = calculate(rules, GAS_STATION, member(),
                new FuelContext(FuelType.GASOLINE, "92#", new BigDecimal("230.00"), BigDecimal.ZERO),
                List.of(), LocalDate.of(2026, 7, 11));
        CalculationResult diesel = calculate(rules, GAS_STATION, member(),
                new FuelContext(FuelType.DIESEL, null, new BigDecimal("280.00"), BigDecimal.ZERO),
                List.of(), LocalDate.of(2026, 7, 11));

        assertThat(candidate(gasoline, "abv2-e1-gasoline-gift-coupons").coupons()).hasSize(2);
        assertThat(candidate(diesel, "abv2-e1-diesel-gift-coupons").coupons()).hasSize(2);
    }

    @Test
    void e2ConfirmedTierRequiresWholeCaseQuantity() {
        String ruleId = "abv2-e2-ilite-250-case-coupon";
        CalculationResult hit = calculate(List.of(rule(ruleId)), GAS_STATION, member(), FuelContext.empty(),
                List.of(syntheticItem("70690981", "伊力特250ml", 10, "68.00", "酒类")),
                LocalDate.of(2026, 7, 11));
        CalculationResult below = calculate(List.of(rule(ruleId)), GAS_STATION, member(), FuelContext.empty(),
                List.of(syntheticItem("70690981", "伊力特250ml", 9, "68.00", "酒类")),
                LocalDate.of(2026, 7, 11));

        assertThat(candidate(hit, ruleId).coupons()).singleElement()
                .satisfies(coupon -> assertThat(coupon.quantity()).isEqualTo(2));
        assertThat(below.blockedPromotions()).anySatisfy(blocked ->
                assertThat(blocked.reasons()).anySatisfy(reason -> assertThat(reason.message()).contains("数量")));
    }

    @Test
    void e2Completes500mlWholeCaseCouponRules() {
        CalculationResult jia = calculate(List.of(rule("abv2-e2-ilite-500-jia-case-coupon")),
                GAS_STATION, member(), FuelContext.empty(),
                List.of(syntheticItem("70690872", "Ilite 500ml Jia", 6, "118.00", "alcohol")),
                LocalDate.of(2026, 7, 11));
        CalculationResult li = calculate(List.of(rule("abv2-e2-ilite-500-li-case-coupon")),
                GAS_STATION, member(), FuelContext.empty(),
                List.of(syntheticItem("70690982", "Ilite 500ml Li", 6, "298.00", "alcohol")),
                LocalDate.of(2026, 7, 11));

        assertThat(candidate(jia, "abv2-e2-ilite-500-jia-case-coupon").coupons()).singleElement()
                .satisfies(coupon -> assertThat(coupon.quantity()).isEqualTo(2));
        assertThat(candidate(li, "abv2-e2-ilite-500-li-case-coupon").coupons()).singleElement()
                .satisfies(coupon -> assertThat(coupon.quantity()).isEqualTo(4));
    }

    @Test
    void f1CngAndLngRulesReturnConfiguredWaterQuantities() {
        List<PromotionRule> rules = List.of(rule("abv2-f1-cng-gift-water"), rule("abv2-f1-lng-gift-water"));
        CalculationResult cng = calculate(rules, GAS_FILLING_STATION, CustomerContext.anonymous(),
                new FuelContext(FuelType.CNG, null, new BigDecimal("50.00"), BigDecimal.ZERO),
                List.of(), LocalDate.of(2026, 7, 11));
        CalculationResult lng = calculate(rules, GAS_FILLING_STATION, CustomerContext.anonymous(),
                new FuelContext(FuelType.LNG, null, new BigDecimal("1000.00"), BigDecimal.ZERO),
                List.of(), LocalDate.of(2026, 7, 11));

        assertThat(candidate(cng, "abv2-f1-cng-gift-water").gifts().getFirst().quantity()).isEqualTo(2);
        assertThat(candidate(lng, "abv2-f1-lng-gift-water").gifts().getFirst().quantity()).isEqualTo(4);
    }

    @Test
    void h2HasEighteenConfirmedDatabaseRules() {
        assertThat(confirmedRules()).filteredOn(rule -> rule.ruleId().startsWith("abv2-h2-"))
                .hasSize(18)
                .allSatisfy(rule -> assertThat(rule.version())
                        .isIn("activity-board-v2", "activity-board-v2-focus"));
    }

    private CalculationResult calculate(
            List<PromotionRule> rules,
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            List<com.cnpc.promoretail.ruleengine.context.CartItem> items,
            LocalDate date
    ) {
        return promotionEngine.calculate(order(station, customer, fuel, items, date, LocalTime.of(10, 0), List.of()),
                rules);
    }

    private PromotionRule rule(String ruleId) {
        return confirmedRules().stream()
                .filter(rule -> rule.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("confirmed rule not found: " + ruleId));
    }

    private CustomerContext member() {
        return new CustomerContext(true, "GOLD", List.of());
    }
}
