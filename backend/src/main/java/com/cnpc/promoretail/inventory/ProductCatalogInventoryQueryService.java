package com.cnpc.promoretail.inventory;

import com.cnpc.promoretail.product.repository.ProductCatalogRepository;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev-db", "postgres"})
public class ProductCatalogInventoryQueryService implements InventoryQueryService {

    private final ProductCatalogRepository productCatalogRepository;

    public ProductCatalogInventoryQueryService(ProductCatalogRepository productCatalogRepository) {
        this.productCatalogRepository = productCatalogRepository;
    }

    @Override
    public BigDecimal getAvailableQuantity(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        return productCatalogRepository.findInventoryQuantity(productCode).orElse(BigDecimal.ZERO);
    }
}
