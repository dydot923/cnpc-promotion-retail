package com.cnpc.promoretail.product.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductCatalogItem(
        String productCode,
        String barcode,
        String productName,
        String category,
        BigDecimal unitPrice,
        BigDecimal inventoryQuantity,
        boolean demoData
) {

    public ProductCatalogItem {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode is required");
        }
        productName = productName == null ? "" : productName;
        unitPrice = money(unitPrice);
        inventoryQuantity = quantity(inventoryQuantity);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
