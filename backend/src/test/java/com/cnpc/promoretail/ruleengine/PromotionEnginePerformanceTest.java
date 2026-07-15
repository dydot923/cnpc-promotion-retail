package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromotionEnginePerformanceTest extends PostgresIntegrationTestSupport {

    private static final int CART_SIZE = 50;
    private static final int MEASURED_RUNS = 10;

    @Test
    void fiftyProductsAgainstAllConfirmedRulesAverageUnderOneSecond() {
        List<PromotionRule> rules = confirmedRules();
        List<ProductCatalogItem> products = productCatalogRepository.search("", CART_SIZE);
        assertThat(products).hasSize(CART_SIZE);
        List<CartItem> items = products.stream().map(product -> item(product, 1)).toList();
        OrderContext context = new OrderContext(
                new StationContext("station-performance", "gas_station", "新疆"),
                new CustomerContext(true, "GOLD", List.of()),
                FuelContext.empty(),
                items,
                LocalDate.of(2026, 7, 9),
                LocalTime.of(10, 0),
                List.of()
        );

        Measurement allConfirmed = measure(context, rules);
        List<PromotionRule> capacityRules = expandToCapacity(rules, 500);
        Measurement capacity = measure(context, capacityRules);

        System.out.printf("PERFORMANCE_BASELINE confirmedRules=%d cartItems=%d runs=%d "
                        + "allConfirmedAverageMs=%d capacityRules=%d capacityAverageMs=%d%n",
                rules.size(), items.size(), MEASURED_RUNS, allConfirmed.averageMillis(),
                capacityRules.size(), capacity.averageMillis());
        assertThat(allConfirmed.lastResult().originalPriceFallback()).isNotNull();
        assertThat(capacity.lastResult().originalPriceFallback()).isNotNull();
        assertThat(allConfirmed.averageMillis()).isLessThan(1000L);
        assertThat(capacity.averageMillis()).isLessThan(1000L);
    }

    private Measurement measure(OrderContext context, List<PromotionRule> rules) {
        for (int warmup = 0; warmup < 3; warmup++) {
            promotionEngine.calculate(context, rules);
        }
        long totalNanos = 0L;
        CalculationResult lastResult = null;
        for (int run = 0; run < MEASURED_RUNS; run++) {
            long started = System.nanoTime();
            lastResult = promotionEngine.calculate(context, rules);
            totalNanos += System.nanoTime() - started;
        }
        return new Measurement(totalNanos / MEASURED_RUNS / 1_000_000L, lastResult);
    }

    private List<PromotionRule> expandToCapacity(List<PromotionRule> source, int targetSize) {
        List<PromotionRule> expanded = new ArrayList<>(targetSize);
        for (int index = 0; index < targetSize; index++) {
            PromotionRule original = source.get(index % source.size());
            expanded.add(new PromotionRule(
                    original.ruleId() + "-capacity-" + index,
                    original.activityName(),
                    original.ruleType(),
                    original.priority(),
                    original.exclusiveGroup(),
                    original.stackable(),
                    original.status(),
                    original.condition(),
                    original.benefit(),
                    original.version()
            ));
        }
        return List.copyOf(expanded);
    }

    private record Measurement(long averageMillis, CalculationResult lastResult) {
    }
}
