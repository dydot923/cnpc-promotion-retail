package com.cnpc.promoretail.support;

import com.cnpc.promoretail.importcenter.ImportCenterService;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.coupon.CouponRepository;
import com.cnpc.promoretail.promotion.productgroup.ProductGroupService;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.PromotionEngine;
import com.cnpc.promoretail.ruleengine.context.CartItem;
import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("dev-db")
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PostgresIntegrationTestSupport {

    private static final AtomicBoolean CATALOG_IMPORTED = new AtomicBoolean();
    private static final AtomicBoolean STOP_HOOK_REGISTERED = new AtomicBoolean();

    protected static final PostgreSQLContainer<?> POSTGRES = SharedPostgresContainer.INSTANCE;

    @Autowired
    protected PromotionEngine promotionEngine;

    @Autowired
    protected PromotionRuleRepository promotionRuleRepository;

    @Autowired
    protected ProductCatalogRepository productCatalogRepository;

    @Autowired
    protected CouponRepository couponRepository;

    @Autowired
    protected ProductGroupService productGroupService;

    @Autowired
    private ImportCenterService importCenterService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
            if (STOP_HOOK_REGISTERED.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop, "postgres-testcontainer-stop"));
            }
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @BeforeAll
    void importRealCatalogDataOnce() {
        if (CATALOG_IMPORTED.compareAndSet(false, true)) {
            importCenterService.importPrices(dataFile("价格.xlsx"));
            importCenterService.importInventory(dataFile("库存.xlsx"));
        }
    }

    protected List<PromotionRule> confirmedRules() {
        return promotionRuleRepository.findConfirmedRules();
    }

    protected CartItem item(ProductCatalogItem product, int quantity) {
        return item(product, quantity, product.unitPrice());
    }

    protected CartItem item(ProductCatalogItem product, int quantity, BigDecimal unitPrice) {
        return new CartItem(
                "line-" + product.productCode(),
                product.productCode(),
                product.barcode(),
                product.productName(),
                quantity,
                unitPrice,
                product.category(),
                product.inventoryQuantity()
        );
    }

    protected CartItem syntheticItem(
            String productCode,
            String name,
            int quantity,
            String unitPrice,
            String category
    ) {
        return new CartItem("line-" + productCode, productCode, null, name, quantity,
                new BigDecimal(unitPrice), category, new BigDecimal("999999"));
    }

    protected OrderContext order(
            StationContext station,
            CustomerContext customer,
            FuelContext fuel,
            List<CartItem> cartItems,
            LocalDate date,
            LocalTime time,
            List<Coupon> coupons
    ) {
        return new OrderContext(station, customer, fuel, cartItems, date, time, coupons);
    }

    protected CalculationResult calculate(OrderContext context) {
        return promotionEngine.calculate(context, confirmedRules());
    }

    protected PromotionCandidate candidate(CalculationResult result, String ruleId) {
        return result.availableCandidates().stream()
                .filter(candidate -> candidate.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("candidate not found for rule " + ruleId));
    }

    protected Path dataFile(String fileName) {
        return Path.of("..", "data", fileName).toAbsolutePath().normalize();
    }

    private static final class SharedPostgresContainer extends PostgreSQLContainer<SharedPostgresContainer> {

        private static final SharedPostgresContainer INSTANCE =
                new SharedPostgresContainer().withDatabaseName("promotion_verification");

        private SharedPostgresContainer() {
            super("postgres:16-alpine");
        }

    }
}
