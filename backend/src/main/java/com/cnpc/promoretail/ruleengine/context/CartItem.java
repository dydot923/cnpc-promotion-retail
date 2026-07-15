package com.cnpc.promoretail.ruleengine.context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public record CartItem(
        String lineId,
        String productCode,
        String barcode,
        String name,
        int quantity,
        BigDecimal unitPrice,
        String category,
        BigDecimal inventoryQuantity
) {

    public CartItem {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode must be provided as String");
        }
        if (barcode != null && barcode.isBlank()) {
            barcode = null;
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice must be provided as BigDecimal");
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean matchesProductScope(Set<String> productCodes) {
        return productCodes == null || productCodes.isEmpty() || productCodes.contains(productCode);
    }

    public boolean excludedByCategory(Set<String> excludedCategories) {
        return category != null && excludedCategories != null && excludedCategories.contains(category);
    }

    public boolean includedByCategory(Set<String> includedCategories) {
        return includedCategories == null
                || includedCategories.isEmpty()
                || (category != null && includedCategories.contains(category));
    }
}
