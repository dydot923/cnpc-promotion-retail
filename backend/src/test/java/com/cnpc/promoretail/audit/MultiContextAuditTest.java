package com.cnpc.promoretail.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultiContextAuditTest extends PostgresIntegrationTestSupport {

    private static final StationContext GAS_STATION = new StationContext("station-001", "gas_station", "新疆");
    private static final StationContext GAS_FILLING_STATION =
            new StationContext("station-cng-001", "gas_filling_station", "新疆");

    @Test
    void executesEightBusinessContextsAgainstAllConfirmedDatabaseRules() {
        Coupon provinceCoupon = couponRepository.findByCouponId("demo-province-half-001").orElseThrow();
        CartItem ninePointNine = ninePointNineItem();

        List<ContextResult> results = List.of(
                audit("1-加油站逢9日非会员", new OrderContext(
                        GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(), List.of(ninePointNine),
                        LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), List.of())),
                audit("2-加气站非逢7日A3", new OrderContext(
                        GAS_FILLING_STATION, member(), FuelContext.empty(),
                        withBase(ninePointNine, syntheticItem("70545523", "small water", 4, "3.00", "包装饮料")),
                        LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), List.of())),
                audit("3-加油站会员柴油500", new OrderContext(
                        GAS_STATION, member(), fuel(FuelType.DIESEL, "500"),
                        withBase(ninePointNine, syntheticItem("70545523", "small water", 4, "3.00", "包装饮料")),
                        LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), List.of())),
                audit("4-加气站逢7日非会员", new OrderContext(
                        GAS_FILLING_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                        withBase(ninePointNine, syntheticItem("70356177", "red bull", 1, "6.00", "包装饮料")),
                        LocalDate.of(2026, 7, 7), LocalTime.of(10, 0), List.of())),
                audit("5-加油站逢8日CN98", new OrderContext(
                        GAS_STATION, CustomerContext.anonymous(),
                        new FuelContext(FuelType.CN98, "CN98", new BigDecimal("10"), BigDecimal.ONE),
                        List.of(ninePointNine), LocalDate.of(2026, 7, 8), LocalTime.of(10, 0), List.of())),
                audit("6-会员省区特色5折券", new OrderContext(
                        GAS_STATION, new CustomerContext(true, "GOLD", List.of(provinceCoupon.couponId())),
                        FuelContext.empty(),
                        withBase(ninePointNine, syntheticItem(provinceCoupon.applicableProductCodes().getFirst(),
                                "coupon item", 1, "20.00", "便利店商品")),
                        LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), List.of(provinceCoupon))),
                audit("7-赛事期间夜间咖啡", new OrderContext(
                        GAS_STATION, member(), FuelContext.empty(),
                        withBase(ninePointNine, syntheticItem("70410278", "coffee", 20, "11.90", "咖啡")),
                        LocalDate.of(2026, 7, 18), LocalTime.of(20, 0), List.of())),
                audit("8-9月月饼礼盒", new OrderContext(
                        GAS_STATION, member(), FuelContext.empty(),
                        withBase(ninePointNine, syntheticItem("70538246", "moon cake", 1, "226.00", "月饼礼盒")),
                        LocalDate.of(2026, 9, 15), LocalTime.of(10, 0), List.of()))
        );

        assertThat(results).hasSize(8).allSatisfy(result -> {
            assertThat(result.calculation().originalPriceFallback()).as(result.name()).isNotNull();
            assertThat(nonFallback(result.calculation())).as(result.name()).isNotEmpty();
            assertThat(nonFallback(result.calculation())).allSatisfy(candidate ->
                    assertThat(candidate.explanation()).isNotBlank());
        });

        assertThat(hasType(results.get(2).calculation(), PromotionRuleType.EXCHANGE_PURCHASE)).isTrue();
        assertThat(hasType(results.get(5).calculation(), PromotionRuleType.COUPON_REDEEM)).isTrue();
        assertThat(hasType(results.get(7).calculation(), PromotionRuleType.COMPOSITE)).isTrue();
        assertThat(hasRule(results.get(0).calculation(), "abv2-g2-day9-gas-station-discount")).isTrue();
        assertThat(candidateByRule(results.get(0).calculation(), "abv2-g2-day9-gas-station-discount")
                .pointsMultiplier()).isEqualTo(3);
        assertThat(hasRule(results.get(1).calculation(), "abv2-a3-gas-filling-discount")).isTrue();
        assertThat(hasRule(results.get(3).calculation(), "abv2-g1-day7-gas-filling-discount")).isTrue();
        assertThat(hasType(results.get(4).calculation(), PromotionRuleType.FUEL_VOLUME_DISCOUNT)).isTrue();
        assertThat(hasRule(results.get(6).calculation(), "abv2-g4-event-night-discount")).isTrue();
    }

    private ContextResult audit(String name, OrderContext context) {
        return new ContextResult(name, promotionEngine.calculate(context, confirmedRules()));
    }

    private CartItem ninePointNineItem() {
        ProductCatalogItem product = confirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith("abv2-99-zone-"))
                .map(rule -> rule.condition().productCodes().iterator().next())
                .map(productCatalogRepository::findByProductCode)
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElseThrow();
        return item(product, 1, new BigDecimal("19.90"));
    }

    private List<CartItem> withBase(CartItem base, CartItem extra) {
        List<CartItem> items = new ArrayList<>();
        items.add(base);
        items.add(extra);
        return items;
    }

    private CustomerContext member() {
        return new CustomerContext(true, "GOLD", List.of());
    }

    private FuelContext fuel(FuelType fuelType, String amount) {
        return new FuelContext(fuelType, null, new BigDecimal(amount), BigDecimal.ZERO);
    }

    private List<PromotionCandidate> nonFallback(CalculationResult result) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleType() != PromotionRuleType.ORIGINAL_PRICE)
                .toList();
    }

    private boolean hasType(CalculationResult result, PromotionRuleType type) {
        return result.availableCandidates().stream().anyMatch(candidate -> candidate.ruleType() == type);
    }

    private boolean hasRule(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream().anyMatch(candidate -> candidate.ruleId().equals(ruleId));
    }

    private PromotionCandidate candidateByRule(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow();
    }

    private record ContextResult(String name, CalculationResult calculation) {
    }
}
