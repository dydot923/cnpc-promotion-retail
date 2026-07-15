package com.cnpc.promoretail.inventory;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.inventory.model.InventoryAlert;
import com.cnpc.promoretail.inventory.model.InventoryAlertRecord;
import com.cnpc.promoretail.inventory.model.InventoryAlertSeverity;
import com.cnpc.promoretail.inventory.repository.InMemoryInventoryAlertRecordRepository;
import com.cnpc.promoretail.inventory.repository.InventoryAlertRecordRepository;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import com.cnpc.promoretail.promotion.repository.PromotionRuleRepository;
import com.cnpc.promoretail.ruleengine.model.BundleItem;
import com.cnpc.promoretail.ruleengine.model.PromotionRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryAlertService {

    private static final BigDecimal SAFETY_STOCK = new BigDecimal("10");
    private static final BigDecimal CRITICAL_STOCK = new BigDecimal("5");

    private final PromotionRuleRepository promotionRuleRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final InventoryAlertRecordRepository inventoryAlertRecordRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public InventoryAlertService(
            PromotionRuleRepository promotionRuleRepository,
            ProductCatalogRepository productCatalogRepository,
            InventoryAlertRecordRepository inventoryAlertRecordRepository,
            AuditLogService auditLogService
    ) {
        this.promotionRuleRepository = promotionRuleRepository;
        this.productCatalogRepository = productCatalogRepository;
        this.inventoryAlertRecordRepository = inventoryAlertRecordRepository;
        this.auditLogService = auditLogService;
    }

    public InventoryAlertService(
            PromotionRuleRepository promotionRuleRepository,
            ProductCatalogRepository productCatalogRepository
    ) {
        this(promotionRuleRepository, productCatalogRepository,
                new InMemoryInventoryAlertRecordRepository(), AuditLogService.noop());
    }

    public List<InventoryAlert> alerts() {
        return mergeRecords(calculatedAlerts());
    }

    public List<InventoryAlert> openAlerts() {
        return alerts().stream()
                .filter(InventoryAlert::open)
                .toList();
    }

    public InventoryAlert handle(String alertId, InventoryAlertHandleRequest request) {
        InventoryAlert alert = alerts().stream()
                .filter(candidate -> candidate.alertId().equals(alertId))
                .findFirst()
                .orElseThrow(() -> new InventoryAlertNotFoundException(alertId));
        InventoryAlertRecord before = inventoryAlertRecordRepository.findByAlertId(alertId).orElse(null);
        InventoryAlertRecord record = InventoryAlertRecord.handled(
                alertId,
                request.operatorId(),
                request.note(),
                before,
                Instant.now()
        );
        inventoryAlertRecordRepository.save(record);
        InventoryAlert after = alert.withRecord(record);
        auditLogService.record("INVENTORY_ALERT_HANDLE", "INVENTORY_ALERT", alertId,
                alert, after, request.operatorId(), "", request.note());
        return after;
    }

    public void linkReplenishmentList(List<InventoryAlert> alerts, String listId, String operatorId) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (InventoryAlert alert : alerts) {
            InventoryAlertRecord previous = inventoryAlertRecordRepository.findByAlertId(alert.alertId()).orElse(null);
            if (previous != null && "HANDLED".equalsIgnoreCase(previous.status())) {
                continue;
            }
            inventoryAlertRecordRepository.save(InventoryAlertRecord.replenishmentCreated(
                    alert.alertId(), listId, operatorId, previous, now));
        }
    }

    private List<InventoryAlert> calculatedAlerts() {
        List<InventoryAlert> alerts = new ArrayList<>();
        for (PromotionRule rule : promotionRuleRepository.findConfirmedRules()) {
            appendRuleProductAlerts(rule, alerts);
            appendGiftItemAlert(rule, alerts);
            appendBundleAlerts(rule, alerts);
        }
        return alerts.stream()
                .sorted(Comparator.comparing(InventoryAlert::severity)
                        .thenComparing(InventoryAlert::productCode)
                        .thenComparing(InventoryAlert::relatedRuleId))
                .toList();
    }

    private List<InventoryAlert> mergeRecords(List<InventoryAlert> alerts) {
        if (alerts.isEmpty()) {
            return alerts;
        }
        Map<String, InventoryAlertRecord> records = inventoryAlertRecordRepository
                .findByAlertIds(alerts.stream().map(InventoryAlert::alertId).toList())
                .stream()
                .collect(Collectors.toMap(InventoryAlertRecord::alertId, Function.identity(), (left, right) -> left));
        return alerts.stream()
                .map(alert -> alert.withRecord(records.get(alert.alertId())))
                .toList();
    }

    private void appendRuleProductAlerts(PromotionRule rule, List<InventoryAlert> alerts) {
        rule.condition().productCodes().forEach(productCode ->
                maybeProductAlert(rule, productCode, SAFETY_STOCK, "促销适用商品库存低于安全库存。")
                        .ifPresent(alerts::add));
    }

    private void appendGiftItemAlert(PromotionRule rule, List<InventoryAlert> alerts) {
        if (rule.benefit().giftItemCode() == null || rule.benefit().giftItemCode().isBlank()) {
            return;
        }
        maybeProductAlert(rule, rule.benefit().giftItemCode(), SAFETY_STOCK, "买赠活动赠品库存低于安全库存。")
                .ifPresent(alerts::add);
    }

    private void appendBundleAlerts(PromotionRule rule, List<InventoryAlert> alerts) {
        if (rule.benefit().bundleItems().isEmpty()) {
            return;
        }

        List<BundleStock> bundleStocks = new ArrayList<>();
        for (BundleItem bundleItem : rule.benefit().bundleItems()) {
            Optional<BigDecimal> inventory = productCatalogRepository.findInventoryQuantity(bundleItem.productCode());
            if (inventory.isEmpty()) {
                product(rule, bundleItem.productCode(), BigDecimal.ZERO)
                        .map(item -> new InventoryAlert(
                                "alert-" + rule.ruleId() + "-" + bundleItem.productCode() + "-bundle-missing",
                                item.productCode(),
                                item.barcode(),
                                item.productName(),
                                item.category(),
                                BigDecimal.ZERO,
                                SAFETY_STOCK.multiply(BigDecimal.valueOf(bundleItem.quantity())),
                                suggested(BigDecimal.ZERO, SAFETY_STOCK.multiply(BigDecimal.valueOf(bundleItem.quantity()))),
                                rule.ruleId(),
                                rule.ruleType(),
                                InventoryAlertSeverity.NO_STATION_STOCK,
                                "组合包商品无本站库存记录，无法确认可组装套数。"))
                        .ifPresent(alerts::add);
                continue;
            }
            BigDecimal quantity = inventory.get();
            BigDecimal availableSets = quantity.divideToIntegralValue(BigDecimal.valueOf(bundleItem.quantity()));
            bundleStocks.add(new BundleStock(bundleItem, quantity, availableSets));
        }

        if (bundleStocks.size() != rule.benefit().bundleItems().size()) {
            return;
        }
        BigDecimal availableSets = bundleStocks.stream()
                .map(BundleStock::availableSets)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (availableSets.compareTo(SAFETY_STOCK) >= 0) {
            return;
        }
        InventoryAlertSeverity severity = severity(availableSets);
        bundleStocks.stream()
                .filter(stock -> stock.availableSets().compareTo(availableSets) == 0)
                .forEach(stock -> product(rule, stock.bundleItem().productCode(), stock.currentQuantity())
                        .map(item -> {
                            BigDecimal productThreshold = SAFETY_STOCK.multiply(
                                    BigDecimal.valueOf(stock.bundleItem().quantity()));
                            return new InventoryAlert(
                                    "alert-" + rule.ruleId() + "-" + item.productCode() + "-bundle",
                                    item.productCode(),
                                    item.barcode(),
                                    item.productName(),
                                    item.category(),
                                    stock.currentQuantity(),
                                    productThreshold,
                                    suggested(stock.currentQuantity(), productThreshold),
                                    rule.ruleId(),
                                    rule.ruleType(),
                                    severity,
                                    "组合包可组装 " + availableSets.toPlainString()
                                            + " 套，限制商品每套需要 " + stock.bundleItem().quantity() + " 件。");
                        })
                        .ifPresent(alerts::add));
    }

    private Optional<InventoryAlert> maybeProductAlert(
            PromotionRule rule,
            String productCode,
            BigDecimal threshold,
            String reason
    ) {
        Optional<BigDecimal> inventory = productCatalogRepository.findInventoryQuantity(productCode);
        if (inventory.isEmpty()) {
            return product(rule, productCode, BigDecimal.ZERO)
                    .map(item -> new InventoryAlert(
                            "alert-" + rule.ruleId() + "-" + productCode + "-missing",
                            item.productCode(),
                            item.barcode(),
                            item.productName(),
                            item.category(),
                            BigDecimal.ZERO,
                            threshold,
                            suggested(BigDecimal.ZERO, threshold),
                            rule.ruleId(),
                            rule.ruleType(),
                            InventoryAlertSeverity.NO_STATION_STOCK,
                            "活动商品本站无库存记录。"));
        }

        BigDecimal current = inventory.get();
        if (current.compareTo(threshold) >= 0) {
            return Optional.empty();
        }
        return product(rule, productCode, current)
                .map(item -> new InventoryAlert(
                        "alert-" + rule.ruleId() + "-" + productCode,
                        item.productCode(),
                        item.barcode(),
                        item.productName(),
                        item.category(),
                        current,
                        threshold,
                        suggested(current, threshold),
                        rule.ruleId(),
                        rule.ruleType(),
                        severity(current),
                        reason));
    }

    private Optional<ProductCatalogItem> product(PromotionRule rule, String productCode, BigDecimal quantity) {
        return Optional.of(productCatalogRepository.findByProductCode(productCode)
                .orElseGet(() -> new ProductCatalogItem(productCode, null, productNameFallback(rule, productCode),
                        null, BigDecimal.ZERO, quantity, false)));
    }

    private String productNameFallback(PromotionRule rule, String productCode) {
        if (productCode.equals(rule.benefit().giftItemCode()) && rule.benefit().giftItemName() != null) {
            return rule.benefit().giftItemName();
        }
        return productCode;
    }

    private BigDecimal suggested(BigDecimal current, BigDecimal threshold) {
        return threshold.multiply(new BigDecimal("2")).subtract(current).max(BigDecimal.ZERO);
    }

    private InventoryAlertSeverity severity(BigDecimal current) {
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return InventoryAlertSeverity.OUT_OF_STOCK;
        }
        if (current.compareTo(CRITICAL_STOCK) < 0) {
            return InventoryAlertSeverity.CRITICAL;
        }
        return InventoryAlertSeverity.LOW;
    }

    private record BundleStock(
            BundleItem bundleItem,
            BigDecimal currentQuantity,
            BigDecimal availableSets
    ) {
    }
}
