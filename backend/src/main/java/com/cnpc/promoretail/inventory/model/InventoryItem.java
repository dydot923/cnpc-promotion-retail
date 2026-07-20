package com.cnpc.promoretail.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InventoryItem(
        String productCode,
        String barcode,
        String productName,
        String category,
        BigDecimal currentQuantity,
        BigDecimal safetyStock,
        BigDecimal suggestedReplenishmentQuantity,
        String stockStatus
) {

    public InventoryItem {
        currentQuantity = quantity(currentQuantity);
        safetyStock = quantity(safetyStock);
        suggestedReplenishmentQuantity = quantity(suggestedReplenishmentQuantity);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
