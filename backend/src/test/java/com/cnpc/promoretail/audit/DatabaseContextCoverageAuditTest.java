package com.cnpc.promoretail.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.importcenter.excel.EasyExcelWorkbookReader;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;

class DatabaseContextCoverageAuditTest extends PostgresIntegrationTestSupport {

    private static final StationContext GAS_STATION =
            new StationContext("station-001", "gas_station", "新疆");
    private static final StationContext GAS_FILLING_STATION =
            new StationContext("station-cng-001", "gas_filling_station", "新疆");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<PromotionRule> cachedConfirmedRules;

    @Test
    void reportsFallbackOnlyCountsAcrossFinalDatabaseContexts() {
        List<ProductCatalogItem> products = authoritativeInventoryProducts();
        DatabaseCaliberDiagnosis diagnosis = diagnoseDatabaseCaliber(products);
        List<ContextCoverageResult> contexts = List.of(
                context("默认上下文", products, GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                        LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), 1, "基线"),
                context("逢9日期 + 加油站", products, GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                        LocalDate.of(2026, 7, 9), LocalTime.of(10, 0), 1, "G2 触发"),
                context("加气站 + 逢7日期", products, GAS_FILLING_STATION, CustomerContext.anonymous(),
                        FuelContext.empty(), LocalDate.of(2026, 7, 7), LocalTime.of(10, 0), 1, "G1 触发"),
                context("加气站 + 非逢7日期", products, GAS_FILLING_STATION, CustomerContext.anonymous(),
                        FuelContext.empty(), LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), 1, "A3 触发"),
                context("逢8日期 + CN98", products, GAS_STATION, CustomerContext.anonymous(),
                        new FuelContext(FuelType.CN98, "CN98", new BigDecimal("100.00"), BigDecimal.ONE),
                        LocalDate.of(2026, 7, 8), LocalTime.of(10, 0), 1, "A4 触发"),
                context("赛事期间 + 夜间", products, GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                        LocalDate.of(2026, 7, 18), LocalTime.of(20, 0), 1, "G4-2 触发"),
                context("会员 + 赛事期间", products, GAS_STATION, member(), FuelContext.empty(),
                        LocalDate.of(2026, 7, 18), LocalTime.of(10, 0), 10, "G4-1 触发"),
                context("非促销日期", products, GAS_STATION, CustomerContext.anonymous(), FuelContext.empty(),
                        LocalDate.of(2026, 7, 12), LocalTime.of(10, 0), 1, "仅兜底")
        );
        JClassBreakdown breakdown = classifyDefaultJ(contexts, diagnosis);

        System.out.printf("J_AUDIT_DIAGNOSIS inventoryRows=%d inventoryDistinct=%d inventoryDemoRows=%d "
                        + "productRows=%d productDemoRows=%d dbExtra=%d excelMissing=%d%n",
                diagnosis.inventoryRows(), diagnosis.inventoryDistinctCodes(), diagnosis.inventoryDemoRows(),
                diagnosis.productRows(), diagnosis.productDemoRows(), diagnosis.dbExtraProductCodes().size(),
                diagnosis.excelMissingProductCodes().size());
        System.out.printf("J_AUDIT_DB_EXTRA codes=%s%n", diagnosis.dbExtraProductCodes());
        for (ContextCoverageResult context : contexts) {
            System.out.printf("J_AUDIT_CONTEXT name=%s base=%d fallbackOnly=%d nonFallback=%d note=%s%n",
                    context.name(), context.baseProducts(), context.fallbackOnlyProducts(),
                    context.nonFallbackProducts(), context.note());
        }
        System.out.printf("J_AUDIT_BREAKDOWN outside99=%d improvable=%d trueGap=%d defaultJ=%d nonJ=%d%n",
                breakdown.reasonableOutsideInventoryCount(), breakdown.improvableDefaultJ(),
                breakdown.trueGapDefaultJ(), breakdown.defaultJ(), breakdown.nonJ());

        ContextCoverageResult defaultContext = contexts.getFirst();
        assertThat(products).hasSize(454);
        assertThat(diagnosis.inventoryNonDemoDistinctCodes()).isEqualTo(464);
        assertThat(diagnosis.dbExtraProductCodes()).hasSize(17);
        assertThat(diagnosis.excelMissingProductCodes()).isEmpty();
        assertThat(contexts).hasSize(8);
        assertThat(contexts.get(1).fallbackOnlyProducts()).isLessThan(defaultContext.fallbackOnlyProducts());
        assertThat(contexts.get(2).fallbackOnlyProducts()).isLessThan(defaultContext.fallbackOnlyProducts());
        assertThat(contexts.get(3).fallbackOnlyProducts()).isLessThan(defaultContext.fallbackOnlyProducts());
        assertThat(contexts.get(4).fallbackOnlyProducts()).isLessThan(defaultContext.fallbackOnlyProducts());
        assertThat(contexts.get(5).fallbackOnlyProducts()).isLessThan(defaultContext.fallbackOnlyProducts());
    }

    private List<ProductCatalogItem> authoritativeInventoryProducts() {
        List<String> productCodes = new EasyExcelWorkbookReader().readSheet(dataFile("库存.xlsx"), 0, 1)
                .stream()
                .map(row -> row.cell(0).trim())
                .distinct()
                .toList();
        assertThat(productCodes).hasSize(454);
        List<ProductCatalogItem> products = productCodes.stream()
                .map(productCatalogRepository::findByProductCode)
                .flatMap(java.util.Optional::stream)
                .toList();
        assertThat(products).hasSize(productCodes.size());
        return products;
    }

    private DatabaseCaliberDiagnosis diagnoseDatabaseCaliber(List<ProductCatalogItem> authoritativeProducts) {
        Set<String> authoritativeCodes = authoritativeProducts.stream()
                .map(ProductCatalogItem::productCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> dbInventoryCodes = jdbcTemplate.queryForList("""
                select product_code
                from inventory_snapshot
                group by product_code
                order by product_code
                """, String.class);
        List<String> dbExtraProductCodes = dbInventoryCodes.stream()
                .filter(code -> !authoritativeCodes.contains(code))
                .toList();
        List<String> excelMissingProductCodes = authoritativeCodes.stream()
                .filter(code -> !dbInventoryCodes.contains(code))
                .toList();
        return new DatabaseCaliberDiagnosis(
                count("select count(*) from inventory_snapshot"),
                count("select count(distinct product_code) from inventory_snapshot"),
                count("select count(*) from inventory_snapshot where is_demo_data = true"),
                count("select count(distinct product_code) from inventory_snapshot where is_demo_data = true"),
                count("select count(*) from inventory_snapshot where is_demo_data = false"),
                count("select count(distinct product_code) from inventory_snapshot where is_demo_data = false"),
                count("select count(*) from product"),
                count("select count(*) from product where is_demo_data = true"),
                count("select count(*) from product where is_demo_data = false"),
                dbExtraProductCodes,
                excelMissingProductCodes,
                ninePointNineOutsideInventoryCount(authoritativeCodes)
        );
    }

    private int ninePointNineOutsideInventoryCount(Set<String> authoritativeCodes) {
        Set<String> ninePointNineCodes = cachedConfirmedRules().stream()
                .filter(rule -> rule.ruleId().startsWith("abv2-99-zone-"))
                .flatMap(rule -> rule.condition().productCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ninePointNineCodes.removeAll(authoritativeCodes);
        return ninePointNineCodes.size();
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private ContextCoverageResult context(
            String name,
            List<ProductCatalogItem> products,
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            LocalDate date,
            LocalTime time,
            int quantity,
            String note
    ) {
        Set<String> fallbackOnlyCodes = new LinkedHashSet<>();
        Set<String> nonFallbackCodes = new LinkedHashSet<>();
        for (ProductCatalogItem product : products) {
            OrderContext context = order(station, customer, fuel, List.of(item(product, quantity)),
                    date, time, List.of());
            boolean hasPromotion = promotionEngine.calculate(context, cachedConfirmedRules()).availableCandidates().stream()
                    .anyMatch(candidate -> candidate.ruleType() != PromotionRuleType.ORIGINAL_PRICE);
            if (!hasPromotion) {
                fallbackOnlyCodes.add(product.productCode());
            } else {
                nonFallbackCodes.add(product.productCode());
            }
        }
        return new ContextCoverageResult(name, products.size(), fallbackOnlyCodes, nonFallbackCodes, note);
    }

    private List<PromotionRule> cachedConfirmedRules() {
        if (cachedConfirmedRules == null) {
            cachedConfirmedRules = confirmedRules();
        }
        return cachedConfirmedRules;
    }

    private JClassBreakdown classifyDefaultJ(
            List<ContextCoverageResult> contexts,
            DatabaseCaliberDiagnosis diagnosis
    ) {
        Set<String> defaultJCodes = contexts.getFirst().fallbackOnlyProductCodes();
        Set<String> promotedInBusinessContext = contexts.stream()
                .skip(1)
                .flatMap(context -> context.nonFallbackProductCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int improvable = (int) defaultJCodes.stream()
                .filter(promotedInBusinessContext::contains)
                .count();
        int trueGap = defaultJCodes.size() - improvable;
        return new JClassBreakdown(diagnosis.ninePointNineOutsideInventory(), improvable, trueGap,
                defaultJCodes.size(), contexts.getFirst().nonFallbackProducts());
    }

    private CustomerContext member() {
        return new CustomerContext(true, "GOLD", List.of());
    }

    private record DatabaseCaliberDiagnosis(
            int inventoryRows,
            int inventoryDistinctCodes,
            int inventoryDemoRows,
            int inventoryDemoDistinctCodes,
            int inventoryNonDemoRows,
            int inventoryNonDemoDistinctCodes,
            int productRows,
            int productDemoRows,
            int productNonDemoRows,
            List<String> dbExtraProductCodes,
            List<String> excelMissingProductCodes,
            int ninePointNineOutsideInventory
    ) {
    }

    private record ContextCoverageResult(
            String name,
            int baseProducts,
            Set<String> fallbackOnlyProductCodes,
            Set<String> nonFallbackProductCodes,
            String note
    ) {
        int fallbackOnlyProducts() {
            return fallbackOnlyProductCodes.size();
        }

        int nonFallbackProducts() {
            return nonFallbackProductCodes.size();
        }
    }

    private record JClassBreakdown(
            int reasonableOutsideInventoryCount,
            int improvableDefaultJ,
            int trueGapDefaultJ,
            int defaultJ,
            int nonJ
    ) {
    }
}
