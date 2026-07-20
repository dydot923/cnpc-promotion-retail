package com.cnpc.promoretail.product.repository;

import com.cnpc.promoretail.importcenter.model.ImportVersion;
import com.cnpc.promoretail.importcenter.model.InventoryImportRow;
import com.cnpc.promoretail.importcenter.model.PriceImportRow;
import com.cnpc.promoretail.product.model.ProductCatalogItem;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductCatalogRepository {

    void savePriceRows(ImportVersion importVersion, List<PriceImportRow> rows);

    void saveInventoryRows(ImportVersion importVersion, List<InventoryImportRow> rows);

    Optional<ProductCatalogItem> findByProductCode(String productCode);

    Optional<ProductCatalogItem> findByBarcode(String barcode);

    List<ProductCatalogItem> search(String keyword, int limit);

    List<ProductCatalogItem> searchInventory(String keyword, int limit);

    List<ProductCatalogItem> findByProductCodes(Collection<String> productCodes);

    Optional<BigDecimal> findInventoryQuantity(String productCode);

    void saveInventoryQuantity(String productCode, BigDecimal quantity, String importVersion);
}
