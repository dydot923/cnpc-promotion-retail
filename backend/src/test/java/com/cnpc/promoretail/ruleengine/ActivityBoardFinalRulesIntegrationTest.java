package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.BlockedPromotion;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityBoardFinalRulesIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String A4 = "abv2-a4-cn98-volume-discount";
    private static final String G1 = "abv2-g1-day7-gas-filling-discount";
    private static final String G2 = "abv2-g2-day9-gas-station-discount";
    private static final String A3 = "abv2-a3-gas-filling-discount";
    private static final String G4_COUPON = "abv2-g4-event-beer-coupon";
    private static final String G4_DISCOUNT = "abv2-g4-event-night-discount";
    private static final String A5_1000_NORMAL = "abv2-a5-day10-super-1000-normal";
    private static final String A5_1000_GOLD = "abv2-a5-day10-super-1000-gold";
    private static final String A5_2000_NORMAL = "abv2-a5-day10-super-2000-normal";
    private static final String A5_2000_GOLD = "abv2-a5-day10-super-2000-gold";
    private static final String A6_SMALL_RECHARGE = "abv2-a6-small-recharge-666";
    private static final String G7_SAFE_PRICE = "audit-personalized-fixed-70485561";
    private static final StationContext GAS_STATION =
            new StationContext("station-001", "gas_station", "新疆");
    private static final StationContext GAS_FILLING_STATION =
            new StationContext("station-cng-001", "gas_filling_station", "新疆");

    @Test
    void a4CoversHitTypeDateAndVolumeBoundaries() {
        CalculationResult oneLiter = calculate(List.of(rule(A4)), GAS_STATION, anonymous(),
                fuel(FuelType.CN98, "CN98", "1.00"), "普通食品", "100.00",
                LocalDate.of(2026, 7, 8), LocalTime.of(10, 0));
        CalculationResult tenLiters = calculate(List.of(rule(A4)), GAS_STATION, anonymous(),
                fuel(FuelType.CN98, "CN98", "10.00"), "普通食品", "100.00",
                LocalDate.of(2026, 7, 18), LocalTime.of(10, 0));
        CalculationResult wrongFuel = calculate(List.of(rule(A4)), GAS_STATION, anonymous(),
                fuel(FuelType.GASOLINE, "92#", "1.00"), "普通食品", "100.00",
                LocalDate.of(2026, 7, 8), LocalTime.of(10, 0));
        CalculationResult wrongDate = calculate(List.of(rule(A4)), GAS_STATION, anonymous(),
                fuel(FuelType.CN98, "CN98", "1.00"), "普通食品", "100.00",
                LocalDate.of(2026, 7, 9), LocalTime.of(10, 0));
        CalculationResult belowVolume = calculate(List.of(rule(A4)), GAS_STATION, anonymous(),
                fuel(FuelType.CN98, "CN98", "0.50"), "普通食品", "100.00",
                LocalDate.of(2026, 7, 28), LocalTime.of(10, 0));

        assertThat(candidate(oneLiter, A4).discountAmount()).isEqualByComparingTo("0.80");
        assertThat(candidate(tenLiters, A4).discountAmount()).isEqualByComparingTo("8.00");
        assertBlocked(wrongFuel, A4, "油品类型");
        assertBlocked(wrongDate, A4, "非活动日期");
        assertBlocked(belowVolume, A4, "油品升数未满1升");
    }

    @Test
    void g1CoversDateStationAndExcludedCategories() {
        CalculationResult hit = storeDiscount(G1, GAS_FILLING_STATION, "零食", LocalDate.of(2026, 7, 7));
        CalculationResult wrongStation = storeDiscount(G1, GAS_STATION, "零食", LocalDate.of(2026, 7, 7));
        CalculationResult wrongDate = storeDiscount(G1, GAS_FILLING_STATION, "零食", LocalDate.of(2026, 7, 8));
        CalculationResult cigarette = storeDiscount(G1, GAS_FILLING_STATION, "香烟", LocalDate.of(2026, 7, 17));
        CalculationResult fertilizer = storeDiscount(G1, GAS_FILLING_STATION, "化肥", LocalDate.of(2026, 7, 27));

        assertThat(candidate(hit, G1).discountAmount()).isEqualByComparingTo("10.00");
        assertBlocked(wrongStation, G1, "站点类型不匹配");
        assertBlocked(wrongDate, G1, "非活动日期");
        assertBlocked(cigarette, G1, "排除品类");
        assertBlocked(fertilizer, G1, "排除品类");
    }

    @Test
    void g2CoversDiscountPointsDateStationAndExclusions() {
        CalculationResult hit = storeDiscount(G2, GAS_STATION, "零食", LocalDate.of(2026, 7, 9));
        CalculationResult wrongStation = storeDiscount(G2, GAS_FILLING_STATION, "零食", LocalDate.of(2026, 7, 9));
        CalculationResult wrongDate = storeDiscount(G2, GAS_STATION, "零食", LocalDate.of(2026, 7, 8));
        CalculationResult cigarette = storeDiscount(G2, GAS_STATION, "香烟", LocalDate.of(2026, 7, 19));
        CalculationResult fertilizer = storeDiscount(G2, GAS_STATION, "化肥", LocalDate.of(2026, 7, 29));

        assertThat(candidate(hit, G2)).satisfies(candidate -> {
            assertThat(candidate.discountAmount()).isEqualByComparingTo("10.00");
            assertThat(candidate.pointsMultiplier()).isEqualTo(3);
            assertThat(candidate.explanation()).isNotBlank();
        });
        assertBlocked(wrongStation, G2, "站点类型不匹配");
        assertBlocked(wrongDate, G2, "非活动日期");
        assertBlocked(cigarette, G2, "排除品类");
        assertBlocked(fertilizer, G2, "排除品类");
    }

    @Test
    void a3CoversAlwaysOnRuleAndG1PriorityConflict() {
        CalculationResult normalDate = storeDiscount(A3, GAS_FILLING_STATION, "零食", LocalDate.of(2026, 7, 8));
        CalculationResult cigarette = storeDiscount(A3, GAS_FILLING_STATION, "香烟", LocalDate.of(2026, 7, 8));
        CalculationResult wrongStation = storeDiscount(A3, GAS_STATION, "零食", LocalDate.of(2026, 7, 8));
        CalculationResult daySeven = calculate(List.of(rule(A3), rule(G1)), GAS_FILLING_STATION, anonymous(),
                FuelContext.empty(), "零食", "100.00", LocalDate.of(2026, 7, 7), LocalTime.of(10, 0));
        CalculationResult nonDaySeven = calculate(List.of(rule(A3), rule(G1)), GAS_FILLING_STATION, anonymous(),
                FuelContext.empty(), "零食", "100.00", LocalDate.of(2026, 7, 8), LocalTime.of(10, 0));

        assertThat(candidate(normalDate, A3).discountAmount()).isEqualByComparingTo("10.00");
        assertBlocked(cigarette, A3, "排除品类");
        assertBlocked(wrongStation, A3, "站点类型不匹配");
        assertThat(nonFallbackRuleIds(daySeven)).contains(G1).doesNotContain(A3);
        assertThat(nonFallbackRuleIds(nonDaySeven)).contains(A3).doesNotContain(G1);
    }

    @Test
    void g4CoversCouponDiscountCrossMidnightAndStacking() {
        List<PromotionRule> rules = List.of(rule(G4_COUPON), rule(G4_DISCOUNT));
        CalculationResult couponHit = calculate(rules, GAS_STATION, member(), FuelContext.empty(),
                "啤酒", "66.00", LocalDate.of(2026, 7, 15), LocalTime.of(10, 0));
        CalculationResult couponBelow = calculate(rules, GAS_STATION, member(), FuelContext.empty(),
                "啤酒", "65.00", LocalDate.of(2026, 7, 15), LocalTime.of(10, 0));
        CalculationResult nightHit = calculate(rules, GAS_STATION, anonymous(), FuelContext.empty(),
                "咖啡", "100.00", LocalDate.of(2026, 7, 15), LocalTime.of(19, 0));
        CalculationResult afterMidnightHit = calculate(rules, GAS_STATION, anonymous(), FuelContext.empty(),
                "咖啡", "100.00", LocalDate.of(2026, 7, 16), LocalTime.of(1, 0));
        CalculationResult daytime = calculate(rules, GAS_STATION, anonymous(), FuelContext.empty(),
                "咖啡", "100.00", LocalDate.of(2026, 7, 15), LocalTime.of(10, 0));
        CalculationResult wrongCategory = calculate(rules, GAS_STATION, anonymous(), FuelContext.empty(),
                "包装饮料", "100.00", LocalDate.of(2026, 7, 15), LocalTime.of(19, 0));
        CalculationResult outsideDate = calculate(rules, GAS_STATION, member(), FuelContext.empty(),
                "啤酒", "100.00", LocalDate.of(2026, 9, 1), LocalTime.of(19, 0));
        CalculationResult stacked = calculate(rules, GAS_STATION, member(), FuelContext.empty(),
                "啤酒", "66.00", LocalDate.of(2026, 7, 15), LocalTime.of(19, 0));

        assertThat(candidate(couponHit, G4_COUPON).coupons()).singleElement().satisfies(coupon -> {
            assertThat(coupon.amount()).isEqualByComparingTo("20.00");
            assertThat(coupon.useThreshold()).isEqualByComparingTo("200.00");
            assertThat(coupon.validDays()).isEqualTo(60);
        });
        assertBlocked(couponBelow, G4_COUPON, "门槛");
        assertThat(candidate(nightHit, G4_DISCOUNT).discountAmount()).isEqualByComparingTo("12.00");
        assertThat(candidate(afterMidnightHit, G4_DISCOUNT).discountAmount()).isEqualByComparingTo("12.00");
        assertBlocked(daytime, G4_DISCOUNT, "活动时段");
        assertBlocked(wrongCategory, G4_DISCOUNT, "品类不在活动范围");
        assertThat(nonFallbackRuleIds(outsideDate)).doesNotContain(G4_COUPON, G4_DISCOUNT);
        assertThat(nonFallbackRuleIds(stacked)).contains(G4_COUPON, G4_DISCOUNT);
    }

    @Test
    void a5CoversRechargeAmountDateAndGoldAddOn() {
        List<PromotionRule> rules = List.of(
                rule(A5_1000_NORMAL),
                rule(A5_1000_GOLD),
                rule(A5_2000_NORMAL),
                rule(A5_2000_GOLD)
        );

        CalculationResult normal1000 = calculateRecharge(rules, "normal", "1000.00", LocalDate.of(2026, 7, 10));
        CalculationResult gold1000 = calculateRecharge(rules, "gold", "1000.00", LocalDate.of(2026, 7, 20));
        CalculationResult platinum2000 = calculateRecharge(rules, "platinum", "2000.00", LocalDate.of(2026, 7, 30));
        CalculationResult below = calculateRecharge(rules, "gold", "999.99", LocalDate.of(2026, 7, 10));
        CalculationResult wrongDate = calculateRecharge(rules, "gold", "1000.00", LocalDate.of(2026, 7, 11));

        assertThat(candidate(normal1000, A5_1000_NORMAL).originalAmount()).isEqualByComparingTo("1000.00");
        assertThat(candidate(normal1000, A5_1000_NORMAL).coupons())
                .extracting(coupon -> coupon.quantity())
                .containsExactly(2, 3, 3);
        assertThat(nonFallbackRuleIds(gold1000)).contains(A5_1000_GOLD).doesNotContain(A5_1000_NORMAL);
        assertThat(candidate(gold1000, A5_1000_GOLD).coupons())
                .anySatisfy(coupon -> {
                    assertThat(coupon.couponTemplateId()).isEqualTo("a5-day10-highgrade-gasoline-15");
                    assertThat(coupon.quantity()).isEqualTo(1);
                });
        assertThat(nonFallbackRuleIds(platinum2000)).contains(A5_2000_GOLD)
                .doesNotContain(A5_1000_NORMAL, A5_1000_GOLD, A5_2000_NORMAL);
        assertThat(candidate(platinum2000, A5_2000_GOLD).coupons())
                .anySatisfy(coupon -> {
                    assertThat(coupon.couponTemplateId()).isEqualTo("a5-day10-highgrade-gasoline-15");
                    assertThat(coupon.quantity()).isEqualTo(2);
                });
        assertBlocked(below, A5_1000_GOLD, "Recharge amount");
        assertThat(nonFallbackRuleIds(wrongDate))
                .doesNotContain(A5_1000_NORMAL, A5_1000_GOLD, A5_2000_NORMAL, A5_2000_GOLD);
    }

    @Test
    void a6SmallRechargeWorksOutsideDay10SuperRechargeDates() {
        List<PromotionRule> rules = List.of(rule(A6_SMALL_RECHARGE));

        CalculationResult hit = calculateRecharge(rules, "normal", "666.00", LocalDate.of(2026, 7, 11));
        CalculationResult excludedDay = calculateRecharge(rules, "normal", "666.00", LocalDate.of(2026, 7, 10));
        CalculationResult below = calculateRecharge(rules, "normal", "665.99", LocalDate.of(2026, 7, 11));

        assertThat(candidate(hit, A6_SMALL_RECHARGE).coupons())
                .extracting(coupon -> coupon.couponTemplateId())
                .containsExactly("small-recharge-gasoline-10", "small-recharge-store-12");
        assertThat(candidate(hit, A6_SMALL_RECHARGE).coupons())
                .extracting(coupon -> coupon.quantity())
                .containsExactly(3, 3);
        assertThat(nonFallbackRuleIds(excludedDay)).doesNotContain(A6_SMALL_RECHARGE);
        assertBlocked(below, A6_SMALL_RECHARGE, "Recharge amount");
    }

    @Test
    void g7FormerZeroDraftIsConfirmedWithExecutableSafePrice() {
        CalculationResult hit = promotionEngine.calculate(order(GAS_STATION, anonymous(), FuelContext.empty(),
                List.of(syntheticItem("70485561", "g7 item", 1, "5.00", "snack")),
                LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), List.of()), List.of(rule(G7_SAFE_PRICE)));
        CalculationResult wrongProduct = promotionEngine.calculate(order(GAS_STATION, anonymous(), FuelContext.empty(),
                List.of(syntheticItem("not-g7", "snack", 1, "5.00", "snack")),
                LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), List.of()), List.of(rule(G7_SAFE_PRICE)));

        assertThat(candidate(hit, G7_SAFE_PRICE).payableAmount()).isEqualByComparingTo("4.25");
        assertThat(candidate(hit, G7_SAFE_PRICE).discountAmount()).isEqualByComparingTo("0.75");
        assertThat(nonFallbackRuleIds(wrongProduct)).doesNotContain(G7_SAFE_PRICE);
    }

    private CalculationResult storeDiscount(String ruleId, StationContext station, String category, LocalDate date) {
        return calculate(List.of(rule(ruleId)), station, anonymous(), FuelContext.empty(), category, "100.00",
                date, LocalTime.of(10, 0));
    }

    private CalculationResult calculate(
            List<PromotionRule> rules,
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            String category,
            String price,
            LocalDate date,
            LocalTime time
    ) {
        return promotionEngine.calculate(order(station, customer, fuel,
                List.of(syntheticItem("test-" + category, category, 1, price, category)),
                date, time, List.of()), rules);
    }

    private CalculationResult calculateRecharge(
            List<PromotionRule> rules,
            String memberLevel,
            String rechargeAmount,
            LocalDate date
    ) {
        return promotionEngine.calculate(new com.cnpc.promoretail.ruleengine.context.OrderContext(
                GAS_STATION,
                new CustomerContext(true, memberLevel, List.of(), null, "E_ENJOY_CARD", "member-a5"),
                FuelContext.empty(),
                List.of(),
                date,
                LocalTime.of(10, 0),
                List.of(),
                new BigDecimal(rechargeAmount)
        ), rules);
    }

    private PromotionRule rule(String ruleId) {
        return confirmedRules().stream()
                .filter(rule -> rule.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("confirmed rule not found: " + ruleId));
    }

    private FuelContext fuel(FuelType type, String grade, String volume) {
        return new FuelContext(type, grade, BigDecimal.ZERO, new BigDecimal(volume));
    }

    private CustomerContext anonymous() {
        return CustomerContext.anonymous();
    }

    private CustomerContext member() {
        return new CustomerContext(true, "GOLD", List.of());
    }

    private List<String> nonFallbackRuleIds(CalculationResult result) {
        return result.availableCandidates().stream()
                .map(PromotionCandidate::ruleId)
                .filter(ruleId -> !"original-price".equals(ruleId))
                .toList();
    }

    private void assertBlocked(CalculationResult result, String ruleId, String reasonText) {
        assertThat(result.blockedPromotions().stream()
                .filter(blocked -> blocked.ruleId().equals(ruleId))
                .map(BlockedPromotion::reasons)
                .flatMap(List::stream)
                .map(reason -> reason.message()))
                .anySatisfy(reason -> assertThat(reason).contains(reasonText));
    }
}
