package com.cnpc.promoretail.product.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.inventory.persistence.entity.InventorySnapshotEntity;
import com.cnpc.promoretail.inventory.persistence.mapper.InventorySnapshotMapper;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import com.cnpc.promoretail.product.persistence.entity.ProductEntity;
import com.cnpc.promoretail.product.persistence.entity.ProductPriceEntity;
import com.cnpc.promoretail.product.persistence.mapper.ProductMapper;
import com.cnpc.promoretail.product.persistence.mapper.ProductPriceMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisProductCatalogRepository implements ProductCatalogRepository {

    private static final String DEFAULT_STATION_CODE = "default";

    private final ProductMapper productMapper;
    private final ProductPriceMapper priceMapper;
    private final InventorySnapshotMapper inventoryMapper;

    public MybatisProductCatalogRepository(
            ProductMapper productMapper,
            ProductPriceMapper priceMapper,
            InventorySnapshotMapper inventoryMapper
    ) {
        this.productMapper = productMapper;
        this.priceMapper = priceMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public void savePriceRows(ImportVersion importVersion, List<PriceImportRow> rows) {
        if (rows == null) {
            return;
        }
        Instant now = Instant.now();
        rows.forEach(row -> {
            upsertProduct(row.productCode(), row.productName(), row.barcode(), null, now);
            ProductPriceEntity price = new ProductPriceEntity();
            price.setProductCode(row.productCode());
            price.setExecutionPrice(row.executionPrice());
            price.setImportVersion(importVersion.value());
            price.setEffectiveAt(now);
            price.setCreatedAt(now);
            price.setDemoData(Boolean.FALSE);
            priceMapper.insert(price);
        });
    }

    @Override
    public void saveInventoryRows(ImportVersion importVersion, List<InventoryImportRow> rows) {
        if (rows == null) {
            return;
        }
        Instant now = Instant.now();
        rows.forEach(row -> {
            upsertProduct(row.productCode(), row.productName(), row.barcode(), null, now);
            InventorySnapshotEntity snapshot = new InventorySnapshotEntity();
            snapshot.setStationCode(DEFAULT_STATION_CODE);
            snapshot.setProductCode(row.productCode());
            snapshot.setQuantity(row.inventoryQuantity());
            snapshot.setImportVersion(importVersion.value());
            snapshot.setSnapshotAt(now);
            snapshot.setDemoData(Boolean.FALSE);
            inventoryMapper.insert(snapshot);
        });
    }

    @Override
    public Optional<ProductCatalogItem> findByProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getProductCode, productCode)))
                .map(this::toCatalogItem);
    }

    @Override
    public Optional<ProductCatalogItem> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getBarcode, barcode)
                        .last("limit 1")))
                .map(this::toCatalogItem);
    }

    @Override
    public List<ProductCatalogItem> search(String keyword, int limit) {
        String text = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<ProductEntity>()
                .orderByAsc(ProductEntity::getProductCode)
                .last("limit " + Math.max(limit, 1));
        if (!text.isBlank()) {
            wrapper.and(w -> w.like(ProductEntity::getProductCode, text)
                    .or()
                    .like(ProductEntity::getBarcode, text)
                    .or()
                    .like(ProductEntity::getProductName, text));
        }
        return productMapper.selectList(wrapper).stream()
                .map(this::toCatalogItem)
                .toList();
    }

    @Override
    public List<ProductCatalogItem> findByProductCodes(Collection<String> productCodes) {
        if (productCodes == null || productCodes.isEmpty()) {
            return List.of();
        }
        return productMapper.selectList(new LambdaQueryWrapper<ProductEntity>()
                        .in(ProductEntity::getProductCode, productCodes)
                        .orderByAsc(ProductEntity::getProductCode))
                .stream()
                .map(this::toCatalogItem)
                .toList();
    }

    @Override
    public Optional<BigDecimal> findInventoryQuantity(String productCode) {
        return latestInventory(productCode).map(InventorySnapshotEntity::getQuantity);
    }

    private void upsertProduct(
            String productCode,
            String productName,
            String barcode,
            String category,
            Instant now
    ) {
        ProductEntity existing = productMapper.selectOne(new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getProductCode, productCode));
        ProductEntity entity = existing == null ? new ProductEntity() : existing;
        entity.setProductCode(productCode);
        entity.setProductName(notBlank(productName) ? productName : entity.getProductName());
        if (notBlank(barcode)) {
            entity.setBarcode(barcode);
        }
        if (notBlank(category)) {
            entity.setCategory(category);
        }
        entity.setCigarette(Boolean.FALSE);
        entity.setFertilizer(Boolean.FALSE);
        entity.setUpdatedAt(now);
        if (existing == null) {
            entity.setCreatedAt(now);
            productMapper.insert(entity);
        } else {
            productMapper.updateById(entity);
        }
    }

    private ProductCatalogItem toCatalogItem(ProductEntity product) {
        return new ProductCatalogItem(
                product.getProductCode(),
                product.getBarcode(),
                product.getProductName(),
                product.getCategory(),
                latestPrice(product.getProductCode()).map(ProductPriceEntity::getExecutionPrice).orElse(BigDecimal.ZERO),
                latestInventory(product.getProductCode()).map(InventorySnapshotEntity::getQuantity).orElse(BigDecimal.ZERO),
                Boolean.TRUE.equals(product.getDemoData())
        );
    }

    private Optional<ProductPriceEntity> latestPrice(String productCode) {
        Optional<ProductPriceEntity> productionPrice = latestPrice(productCode, false);
        if (productionPrice.isPresent()) {
            return productionPrice;
        }
        return latestPrice(productCode, true);
    }

    private Optional<ProductPriceEntity> latestPrice(String productCode, boolean demoData) {
        return Optional.ofNullable(priceMapper.selectOne(new LambdaQueryWrapper<ProductPriceEntity>()
                .eq(ProductPriceEntity::getProductCode, productCode)
                .eq(ProductPriceEntity::getDemoData, demoData)
                .orderByDesc(ProductPriceEntity::getEffectiveAt)
                .orderByDesc(ProductPriceEntity::getId)
                .last("limit 1")));
    }

    private Optional<InventorySnapshotEntity> latestInventory(String productCode) {
        Optional<InventorySnapshotEntity> productionInventory = latestInventory(productCode, false);
        if (productionInventory.isPresent()) {
            return productionInventory;
        }
        return latestInventory(productCode, true);
    }

    private Optional<InventorySnapshotEntity> latestInventory(String productCode, boolean demoData) {
        return Optional.ofNullable(inventoryMapper.selectOne(new LambdaQueryWrapper<InventorySnapshotEntity>()
                .eq(InventorySnapshotEntity::getStationCode, DEFAULT_STATION_CODE)
                .eq(InventorySnapshotEntity::getProductCode, productCode)
                .eq(InventorySnapshotEntity::getDemoData, demoData)
                .orderByDesc(InventorySnapshotEntity::getSnapshotAt)
                .orderByDesc(InventorySnapshotEntity::getId)
                .last("limit 1")));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
