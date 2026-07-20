package com.cnpc.promoretail.inventory;

import com.cnpc.promoretail.audit.AuditLogService;
import com.cnpc.promoretail.inventory.model.InventoryItem;
import com.cnpc.promoretail.inventory.model.InventoryReplenishmentResponse;
import com.cnpc.promoretail.product.ProductNotFoundException;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryManagementService {

    private static final BigDecimal SAFETY_STOCK = new BigDecimal("10");
    private static final BigDecimal TARGET_STOCK = new BigDecimal("20");
    private static final BigDecimal CRITICAL_STOCK = new BigDecimal("5");
    private static final int INVENTORY_QUERY_LIMIT = 2_000;
    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "OUT_OF_STOCK", 1,
            "CRITICAL", 2,
            "LOW", 3,
            "NORMAL", 4
    );

    private final ProductCatalogRepository productCatalogRepository;
    private final AuditLogService auditLogService;

    public InventoryManagementService(
            ProductCatalogRepository productCatalogRepository,
            AuditLogService auditLogService
    ) {
        this.productCatalogRepository = productCatalogRepository;
        this.auditLogService = auditLogService;
    }

    public List<InventoryItem> items(String keyword, String stockStatus) {
        String requestedStatus = stockStatus == null ? "" : stockStatus.trim().toUpperCase();
        return productCatalogRepository.searchInventory(keyword, INVENTORY_QUERY_LIMIT).stream()
                .map(this::toInventoryItem)
                .filter(item -> requestedStatus.isBlank() || requestedStatus.equals(item.stockStatus()))
                .sorted(Comparator
                        .comparingInt((InventoryItem item) -> STATUS_ORDER.getOrDefault(item.stockStatus(), 99))
                        .thenComparing(InventoryItem::currentQuantity)
                        .thenComparing(InventoryItem::productCode))
                .toList();
    }

    @Transactional
    public InventoryReplenishmentResponse replenish(
            String productCode,
            InventoryReplenishmentRequest request
    ) {
        ProductCatalogItem before = productCatalogRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException("未找到补货商品: " + productCode));
        BigDecimal replenishedQuantity = request.quantity();
        BigDecimal quantityAfter = before.inventoryQuantity().add(replenishedQuantity);
        String operationId = "stock-in-" + UUID.randomUUID();
        Instant replenishedAt = Instant.now();

        productCatalogRepository.saveInventoryQuantity(productCode, quantityAfter, operationId);
        InventoryReplenishmentResponse response = new InventoryReplenishmentResponse(
                operationId,
                before.productCode(),
                before.productName(),
                before.inventoryQuantity(),
                replenishedQuantity,
                quantityAfter,
                request.operatorId(),
                request.note(),
                replenishedAt
        );
        auditLogService.record(
                "INVENTORY_REPLENISH",
                "PRODUCT_INVENTORY",
                productCode,
                Map.of("quantity", before.inventoryQuantity()),
                response,
                request.operatorId(),
                "",
                request.note()
        );
        return response;
    }

    private InventoryItem toInventoryItem(ProductCatalogItem product) {
        BigDecimal current = product.inventoryQuantity();
        return new InventoryItem(
                product.productCode(),
                product.barcode(),
                product.productName(),
                product.category(),
                current,
                SAFETY_STOCK,
                TARGET_STOCK.subtract(current).max(BigDecimal.ZERO),
                stockStatus(current)
        );
    }

    private String stockStatus(BigDecimal current) {
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return "OUT_OF_STOCK";
        }
        if (current.compareTo(CRITICAL_STOCK) < 0) {
            return "CRITICAL";
        }
        if (current.compareTo(SAFETY_STOCK) < 0) {
            return "LOW";
        }
        return "NORMAL";
    }
}
