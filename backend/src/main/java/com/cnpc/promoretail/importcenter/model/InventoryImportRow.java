package com.cnpc.promoretail.importcenter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InventoryImportRow(
        String productCode,
        String productName,
        String barcode,
        BigDecimal inventoryQuantity,
        String sheetName,
        int rowNumber
) {

    public InventoryImportRow {
        inventoryQuantity = inventoryQuantity == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : inventoryQuantity.setScale(2, RoundingMode.HALF_UP);
    }
}
