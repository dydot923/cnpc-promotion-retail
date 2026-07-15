package com.cnpc.promoretail.product.repository;

import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryProductCatalogRepository implements ProductCatalogRepository {

    private final ConcurrentMap<String, ProductCatalogItem> products = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> productCodeByBarcode = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> inventoryByProductCode = new ConcurrentHashMap<>();

    @Override
    public void savePriceRows(ImportVersion importVersion, List<PriceImportRow> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(row -> upsert(row.productCode(), row.barcode(), row.productName(), null,
                row.executionPrice(), inventoryByProductCode.get(row.productCode())));
    }

    @Override
    public void saveInventoryRows(ImportVersion importVersion, List<InventoryImportRow> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(row -> {
            inventoryByProductCode.put(row.productCode(), row.inventoryQuantity());
            ProductCatalogItem existing = products.get(row.productCode());
            upsert(row.productCode(), row.barcode(), row.productName(),
                    existing == null ? null : existing.category(),
                    existing == null ? BigDecimal.ZERO : existing.unitPrice(),
                    row.inventoryQuantity());
        });
    }

    @Override
    public Optional<ProductCatalogItem> findByProductCode(String productCode) {
        return Optional.ofNullable(products.get(productCode));
    }

    @Override
    public Optional<ProductCatalogItem> findByBarcode(String barcode) {
        String productCode = productCodeByBarcode.get(barcode);
        return productCode == null ? Optional.empty() : findByProductCode(productCode);
    }

    @Override
    public List<ProductCatalogItem> search(String keyword, int limit) {
        String normalized = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        return products.values().stream()
                .filter(item -> normalized.isBlank()
                        || contains(item.productCode(), normalized)
                        || contains(item.barcode(), normalized)
                        || contains(item.productName(), normalized))
                .sorted(Comparator.comparing(ProductCatalogItem::productCode))
                .limit(Math.max(limit, 1))
                .toList();
    }

    @Override
    public List<ProductCatalogItem> findByProductCodes(Collection<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return List.of();
        }
        return productCodes.stream()
                .distinct()
                .map(this::findByProductCode)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<BigDecimal> findInventoryQuantity(String productCode) {
        return Optional.ofNullable(inventoryByProductCode.get(productCode));
    }

    private void upsert(
            String productCode,
            String barcode,
            String productName,
            String category,
            BigDecimal unitPrice,
            BigDecimal inventoryQuantity
    ) {
        ProductCatalogItem item = new ProductCatalogItem(productCode, barcode, productName, category,
                unitPrice, inventoryQuantity, false);
        products.put(productCode, item);
        if (barcode != null && !barcode.isBlank()) {
            productCodeByBarcode.put(barcode, productCode);
        }
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
