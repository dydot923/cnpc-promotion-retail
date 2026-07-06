package com.cnpc.promoretail.importcenter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PriceImportRow(
        String productCode,
        String productName,
        String barcode,
        BigDecimal executionPrice,
        String sheetName,
        int rowNumber
) {

    public PriceImportRow {
        executionPrice = executionPrice == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : executionPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
