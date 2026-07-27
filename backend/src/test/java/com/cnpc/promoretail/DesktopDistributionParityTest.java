package com.cnpc.promoretail;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.checkout.CheckoutApplicationService;
import com.cnpc.promoretail.checkout.CheckoutCalculateRequest;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledOnOs(OS.WINDOWS)
class DesktopDistributionParityTest {

    private static final String TEST_STATION_CODE = "1-A6501-C001-S001";

    @TempDir
    private Path temporaryDirectory;

    @BeforeEach
    void configureDesktopDataDirectory() {
        System.setProperty("cnpc.desktop", "true");
        System.setProperty(DesktopEmbeddedPostgres.DATA_DIRECTORY_PROPERTY, temporaryDirectory.toString());
        System.setProperty("java.awt.headless", "true");
        System.clearProperty("spring.profiles.active");
    }

    @AfterEach
    void clearDesktopConfiguration() {
        DesktopEmbeddedPostgres.stop();
        List.of(
                "cnpc.desktop",
                DesktopEmbeddedPostgres.DATA_DIRECTORY_PROPERTY,
                "java.awt.headless",
                "spring.profiles.active",
                "spring.datasource.url",
                "spring.datasource.username",
                "spring.datasource.password",
                "spring.flyway.enabled"
        ).forEach(System::clearProperty);
    }

    @Test
    void packagedDatasetMatchesTheCompleteLocalPromotionDataset() {
        DesktopEmbeddedPostgres.configure();
        int priceRowsAfterFirstStart;
        int inventoryRowsAfterFirstStart;
        try (ConfigurableApplicationContext context = startApplication()) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            PromotionRuleRepository rules = context.getBean(PromotionRuleRepository.class);
            ProductCatalogRepository products = context.getBean(ProductCatalogRepository.class);

            assertThat(count(jdbc, "select count(*) from flyway_schema_history where success")).isEqualTo(51);
            assertThat(count(jdbc, "select count(*) from promotion_rule_draft where status = 'CONFIRMED'"))
                    .isEqualTo(369);
            assertThat(count(jdbc, "select count(*) from station")).isEqualTo(297);
            assertThat(count(jdbc, "select count(*) from import_batch where source_file like '%seed-data%'")).isEqualTo(2);

            priceRowsAfterFirstStart = count(jdbc,
                    """
                    select count(*)
                    from product_price
                    where import_version in (
                        select import_version from import_batch
                        where import_type = 'PRICE' and source_file like '%seed-data%'
                    )
                    """);
            inventoryRowsAfterFirstStart = count(jdbc,
                    """
                    select count(*)
                    from inventory_snapshot
                    where import_version in (
                        select import_version from import_batch
                        where import_type = 'INVENTORY' and source_file like '%seed-data%'
                    )
                    """);
            assertThat(priceRowsAfterFirstStart).isEqualTo(12_756);
            assertThat(inventoryRowsAfterFirstStart).isEqualTo(454);

            List<PromotionRule> confirmedRules = rules.findConfirmedRules();
            assertThat(confirmedRules).hasSize(369);
            assertThat(confirmedRules.stream().map(PromotionRule::ruleType).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(Arrays.stream(PromotionRuleType.values())
                            .filter(type -> type != PromotionRuleType.ORIGINAL_PRICE)
                            .toList());
            assertThat(missingProductCodes(confirmedRules, products)).isEmpty();

            assertScreenshotExchangeCalculation(context, products);
            assertCasePriceCalculation(context, products);
        } finally {
            DesktopEmbeddedPostgres.stop();
        }

        DesktopEmbeddedPostgres.configure();
        try (ConfigurableApplicationContext context = startApplication()) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(count(jdbc, """
                    select count(*)
                    from product_price
                    where import_version in (
                        select import_version from import_batch
                        where import_type = 'PRICE' and source_file like '%seed-data%'
                    )
                    """))
                    .isEqualTo(priceRowsAfterFirstStart);
            assertThat(count(jdbc, """
                    select count(*)
                    from inventory_snapshot
                    where import_version in (
                        select import_version from import_batch
                        where import_type = 'INVENTORY' and source_file like '%seed-data%'
                    )
                    """))
                    .isEqualTo(inventoryRowsAfterFirstStart);
            assertThat(count(jdbc, "select count(*) from import_batch where source_file like '%seed-data%'"))
                    .isEqualTo(2);
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(PromotionRetailApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    private void assertScreenshotExchangeCalculation(
            ConfigurableApplicationContext context,
            ProductCatalogRepository products
    ) {
        ProductCatalogItem redBull = products.findByProductCode("70453858").orElseThrow();
        OrderContext order = order(
                item(redBull, 3),
                new FuelContext(FuelType.GASOLINE, "92", new BigDecimal("180.00"), BigDecimal.ZERO),
                LocalDate.of(2026, 7, 27)
        );
        CheckoutCalculateRequest request = new CheckoutCalculateRequest(
                order,
                order.businessDate(),
                order.businessTime(),
                null,
                null,
                null,
                TEST_STATION_CODE,
                false,
                null,
                null,
                null,
                FuelType.GASOLINE,
                new BigDecimal("180.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                null
        );

        var result = context.getBean(CheckoutApplicationService.class).calculate(request);
        assertThat(result.originalAmount()).isEqualByComparingTo("198.00");
        assertThat(result.payableAmount()).isEqualByComparingTo("192.00");
        assertThat(result.availableCandidates())
                .extracting(candidate -> candidate.ruleId())
                .contains("abv2-h2-redbull-gasoline");
    }

    private void assertCasePriceCalculation(
            ConfigurableApplicationContext context,
            ProductCatalogRepository products
    ) {
        ProductCatalogItem water = products.findByProductCode("70545526").orElseThrow();
        OrderContext order = order(item(water, 12), FuelContext.empty(), LocalDate.of(2026, 7, 27));
        CheckoutCalculateRequest request = new CheckoutCalculateRequest(
                order,
                order.businessDate(),
                order.businessTime(),
                null,
                null,
                null,
                TEST_STATION_CODE,
                false,
                null,
                null,
                null,
                FuelType.NONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                null
        );

        var result = context.getBean(CheckoutApplicationService.class).calculate(request);
        assertThat(result.availableCandidates())
                .filteredOn(candidate -> candidate.ruleId().equals("abv2-nono-water-gesang-500-case"))
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.payableAmount()).isEqualByComparingTo("27.90"));
    }

    private OrderContext order(CartItem item, FuelContext fuel, LocalDate date) {
        return new OrderContext(
                StationContext.defaultStation(),
                CustomerContext.anonymous(),
                fuel,
                List.of(item),
                date,
                LocalTime.of(14, 38),
                List.of()
        );
    }

    private CartItem item(ProductCatalogItem product, int quantity) {
        return new CartItem(
                "line-" + product.productCode(),
                product.productCode(),
                product.barcode(),
                product.productName(),
                quantity,
                product.unitPrice(),
                product.category(),
                product.inventoryQuantity()
        );
    }

    private Set<String> missingProductCodes(
            List<PromotionRule> rules,
            ProductCatalogRepository products
    ) {
        Set<String> referencedCodes = rules.stream()
                .flatMap(rule -> rule.condition().productCodes().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> availableCodes = products.findByProductCodes(referencedCodes).stream()
                .map(ProductCatalogItem::productCode)
                .collect(Collectors.toSet());
        referencedCodes.removeAll(availableCodes);
        return referencedCodes;
    }

    private int count(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
