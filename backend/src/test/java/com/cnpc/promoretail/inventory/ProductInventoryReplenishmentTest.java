package com.cnpc.promoretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.audit.DefaultAuditLogService;
import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.audit.repository.InMemoryAuditLogRepository;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.inventory.model.InventoryAlert;
import com.cnpc.promoretail.inventory.model.InventoryAlertSeverity;
import com.cnpc.promoretail.inventory.model.InventoryReplenishmentResponse;
import com.cnpc.promoretail.inventory.repository.InMemoryInventoryAlertRecordRepository;
import com.cnpc.promoretail.product.ProductQueryService;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.InMemoryProductCatalogRepository;
import com.cnpc.promoretail.promotion.model.ImportedPromotionRule;
import com.cnpc.promoretail.promotion.repository.InMemoryPromotionRuleRepository;
import com.cnpc.promoretail.promotion.service.PromotionRuleGovernanceService;
import com.cnpc.promoretail.replenishment.ReplenishmentService;
import com.cnpc.promoretail.replenishment.model.ReplenishmentList;
import com.cnpc.promoretail.replenishment.repository.InMemoryReplenishmentListRepository;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import com.cnpc.promoretail.ruleengine.model.PromotionBenefit;
import com.cnpc.promoretail.ruleengine.model.PromotionCondition;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleStatus;
import com.cnpc.promoretail.ruleengine.model.PromotionRuleType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductInventoryReplenishmentTest {

    private final InMemoryProductCatalogRepository productRepository = new InMemoryProductCatalogRepository();
    private final InMemoryPromotionRuleRepository promotionRepository = new InMemoryPromotionRuleRepository();
    private final PromotionRuleGovernanceService governanceService =
            new PromotionRuleGovernanceService(promotionRepository);

    @BeforeEach
    void setUp() {
        productRepository.savePriceRows(new ImportVersion("price-v1"), List.of(
                price("low-sku", "barcode-low", "低库存商品", "12.00"),
                price("gift-buy-sku", "barcode-gift-buy", "买赠购买商品", "40.00"),
                price("gift-sku", "barcode-gift", "赠品", "3.00"),
                price("bundle-a", "barcode-bundle-a", "组合 A", "20.00"),
                price("bundle-b", "barcode-bundle-b", "组合 B", "20.00"),
                price("exchange-sku", "barcode-exchange", "换购商品", "6.00")
        ));
        productRepository.saveInventoryRows(new ImportVersion("inventory-v1"), List.of(
                inventory("low-sku", "barcode-low", "低库存商品", "8"),
                inventory("gift-buy-sku", "barcode-gift-buy", "买赠购买商品", "20"),
                inventory("gift-sku", "barcode-gift", "赠品", "0"),
                inventory("bundle-a", "barcode-bundle-a", "组合 A", "4"),
                inventory("bundle-b", "barcode-bundle-b", "组合 B", "20")
        ));

        confirm(rule("rule-low", PromotionRuleType.FIXED_PRICE,
                new PromotionCondition(Set.of("low-sku"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.fixedPrice(new BigDecimal("9.90"))));
        confirm(rule("rule-gift", PromotionRuleType.GIFT_ITEM,
                new PromotionCondition(Set.of("gift-buy-sku"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.giftItem("gift-sku", "赠品", 1)));
        confirm(rule("rule-bundle", PromotionRuleType.BUNDLE_PRICE,
                new PromotionCondition(Set.of("bundle-a", "bundle-b"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO),
                PromotionBenefit.bundlePrice("bundle-demo",
                        List.of(new BundleItem("bundle-a", 1), new BundleItem("bundle-b", 2)),
                        new BigDecimal("25.00"))));
        confirm(rule("rule-exchange", PromotionRuleType.EXCHANGE_PURCHASE,
                new PromotionCondition(Set.of("exchange-sku"), Set.of(), Set.of(), Set.of(), Set.of(),
                        null, null, BigDecimal.ZERO, new BigDecimal("200.00"), false, BigDecimal.ZERO),
                PromotionBenefit.exchangePurchase(new BigDecimal("3.00"), 1)));
    }

    @Test
    void productQueryFindsByBarcodeAndSearchesByKeyword() {
        ProductQueryService service = new ProductQueryService(productRepository);

        ProductCatalogItem item = service.findByBarcode("barcode-low");

        assertThat(item.productCode()).isEqualTo("low-sku");
        assertThat(item.productName()).isEqualTo("低库存商品");
        assertThat(item.unitPrice()).isEqualByComparingTo("12.00");
        assertThat(item.inventoryQuantity()).isEqualByComparingTo("8.00");
        assertThat(service.search("组合")).extracting(ProductCatalogItem::productCode)
                .contains("bundle-a", "bundle-b");
    }

    @Test
    void inventoryAlertsCoverSeverityAndBundleAssembly() {
        InventoryAlertService service = new InventoryAlertService(promotionRepository, productRepository);

        List<InventoryAlert> alerts = service.alerts();

        assertThat(alerts).extracting(InventoryAlert::severity)
                .contains(InventoryAlertSeverity.LOW,
                        InventoryAlertSeverity.OUT_OF_STOCK,
                        InventoryAlertSeverity.CRITICAL,
                        InventoryAlertSeverity.NO_STATION_STOCK);
        assertThat(alerts).anySatisfy(alert -> {
            assertThat(alert.productCode()).isEqualTo("low-sku");
            assertThat(alert.severity()).isEqualTo(InventoryAlertSeverity.LOW);
            assertThat(alert.suggestedReplenishmentQuantity()).isEqualByComparingTo("12.00");
        });
        assertThat(alerts).anySatisfy(alert -> {
            assertThat(alert.productCode()).isEqualTo("gift-sku");
            assertThat(alert.severity()).isEqualTo(InventoryAlertSeverity.OUT_OF_STOCK);
        });
        assertThat(alerts).anySatisfy(alert -> {
            assertThat(alert.productCode()).isEqualTo("bundle-a");
            assertThat(alert.severity()).isEqualTo(InventoryAlertSeverity.CRITICAL);
            assertThat(alert.reason()).contains("可组装 4");
        });
        assertThat(alerts).anySatisfy(alert -> {
            assertThat(alert.productCode()).isEqualTo("exchange-sku");
            assertThat(alert.severity()).isEqualTo(InventoryAlertSeverity.NO_STATION_STOCK);
        });
    }

    @Test
    void inventoryAlertCanBeMarkedHandledAndKeepsStatusAfterRefresh() {
        InMemoryInventoryAlertRecordRepository alertRecordRepository = new InMemoryInventoryAlertRecordRepository();
        InMemoryAuditLogRepository auditRepository = new InMemoryAuditLogRepository();
        InventoryAlertService service = new InventoryAlertService(
                promotionRepository,
                productRepository,
                alertRecordRepository,
                new DefaultAuditLogService(auditRepository)
        );

        InventoryAlert alert = service.alerts().stream()
                .filter(candidate -> candidate.productCode().equals("low-sku"))
                .findFirst()
                .orElseThrow();
        InventoryAlert handled = service.handle(alert.alertId(),
                new InventoryAlertHandleRequest("stock-manager", "manual check completed"));

        assertThat(handled.status()).isEqualTo("HANDLED");
        assertThat(handled.handledBy()).isEqualTo("stock-manager");
        assertThat(handled.handleNote()).isEqualTo("manual check completed");
        assertThat(service.alerts()).anySatisfy(refreshed -> {
            assertThat(refreshed.alertId()).isEqualTo(alert.alertId());
            assertThat(refreshed.status()).isEqualTo("HANDLED");
        });
        assertThat(auditRepository.findByEntity("INVENTORY_ALERT", alert.alertId()))
                .extracting(log -> log.actionType())
                .containsExactly("INVENTORY_ALERT_HANDLE");
    }

    @Test
    void inventoryCanBeListedAndReplenishmentWritesBackQuantity() {
        InMemoryAuditLogRepository auditRepository = new InMemoryAuditLogRepository();
        InventoryManagementService inventoryService = new InventoryManagementService(
                productRepository,
                new DefaultAuditLogService(auditRepository)
        );

        assertThat(inventoryService.items("低库存", "LOW"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.productCode()).isEqualTo("low-sku");
                    assertThat(item.currentQuantity()).isEqualByComparingTo("8.00");
                    assertThat(item.suggestedReplenishmentQuantity()).isEqualByComparingTo("12.00");
                });

        InventoryReplenishmentResponse response = inventoryService.replenish(
                "low-sku",
                new InventoryReplenishmentRequest(new BigDecimal("12"), "stock-manager", "到货入库")
        );

        assertThat(response.quantityBefore()).isEqualByComparingTo("8.00");
        assertThat(response.replenishedQuantity()).isEqualByComparingTo("12.00");
        assertThat(response.quantityAfter()).isEqualByComparingTo("20.00");
        assertThat(productRepository.findInventoryQuantity("low-sku")).hasValueSatisfying(
                quantity -> assertThat(quantity).isEqualByComparingTo("20.00")
        );
        assertThat(inventoryService.items("low-sku", "NORMAL"))
                .singleElement()
                .satisfies(item -> assertThat(item.currentQuantity()).isEqualByComparingTo("20.00"));
        assertThat(auditRepository.findByEntity("PRODUCT_INVENTORY", "low-sku"))
                .extracting(log -> log.actionType())
                .containsExactly("INVENTORY_REPLENISH");
    }

    @Test
    void replenishmentListCanBeGeneratedAndExportedAsCsv() {
        InMemoryInventoryAlertRecordRepository alertRecordRepository = new InMemoryInventoryAlertRecordRepository();
        InventoryAlertService alertService = new InventoryAlertService(
                promotionRepository,
                productRepository,
                alertRecordRepository,
                AuditLogService.noop()
        );
        ReplenishmentService replenishmentService = new ReplenishmentService(alertService);
        InventoryAlert openAlert = alertService.openAlerts().getFirst();

        ReplenishmentList list = replenishmentService.createFromCurrentAlerts();
        String csv = new String(replenishmentService.exportCsv(list.listId()), StandardCharsets.UTF_8);

        assertThat(list.listId()).startsWith("repl-");
        assertThat(list.items()).isNotEmpty();
        assertThat(csv).contains("productCode,barcode,productName");
        assertThat(csv).contains("low-sku");
        assertThat(csv).contains("exchange-sku");
        assertThat(alertRecordRepository.findByAlertId(openAlert.alertId())).hasValueSatisfying(record -> {
            assertThat(record.status()).isEqualTo("REPLENISHMENT_CREATED");
            assertThat(record.replenishmentListId()).isEqualTo(list.listId());
        });
        assertThat(alertService.openAlerts()).isEmpty();
    }

    @Test
    void replenishmentListIsPersistedAndAuditedWhenGeneratedAndExported() {
        InventoryAlertService alertService = new InventoryAlertService(promotionRepository, productRepository);
        InMemoryReplenishmentListRepository listRepository = new InMemoryReplenishmentListRepository();
        InMemoryAuditLogRepository auditRepository = new InMemoryAuditLogRepository();
        ReplenishmentService replenishmentService = new ReplenishmentService(
                alertService,
                listRepository,
                new DefaultAuditLogService(auditRepository)
        );

        ReplenishmentList list = replenishmentService.createFromCurrentAlerts("stock-manager");
        replenishmentService.exportCsv(list.listId(), "stock-manager");

        assertThat(listRepository.findByListId(list.listId()))
                .hasValueSatisfying(saved -> assertThat(saved.status()).isEqualTo("EXPORTED"));
        assertThat(auditRepository.findByEntity("REPLENISHMENT_LIST", list.listId()))
                .extracting(log -> log.actionType())
                .containsExactly("REPLENISHMENT_GENERATE", "REPLENISHMENT_EXPORT");
    }

    private void confirm(PromotionRule rule) {
        governanceService.confirmDraft(governanceService.createDraft(
                new ImportedPromotionRule(new ImportVersion("import-" + rule.ruleId()), "manual", 1, rule),
                "tester").draftId(), "manager", "confirm");
    }

    private PromotionRule rule(PromotionRuleType type, PromotionCondition condition, PromotionBenefit benefit) {
        return rule("rule-" + type.name().toLowerCase(), type, condition, benefit);
    }

    private PromotionRule rule(
            String ruleId,
            PromotionRuleType type,
            PromotionCondition condition,
            PromotionBenefit benefit
    ) {
        return new PromotionRule(ruleId, ruleId, type, 50, "inventory-test", false,
                PromotionRuleStatus.PENDING_CONFIRMATION, condition, benefit, "import-v1");
    }

    private PriceImportRow price(String productCode, String barcode, String name, String price) {
        return new PriceImportRow(productCode, name, barcode, new BigDecimal(price), "price", 1);
    }

    private InventoryImportRow inventory(String productCode, String barcode, String name, String quantity) {
        return new InventoryImportRow(productCode, name, barcode, new BigDecimal(quantity), "inventory", 1);
    }
}
