package com.cnpc.promoretail.importcenter.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record FixedPricePromotionImportRow(
        String productCode,
        String productName,
        String category,
        BigDecimal originalPrice,
        int quantity,
        BigDecimal fixedPrice,
        BigDecimal grossMarginRate,
        String sheetName,
        int rowNumber
) {

    public FixedPricePromotionImportRow {
        originalPrice = originalPrice == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : originalPrice.setScale(2, RoundingMode.HALF_UP);
        fixedPrice = fixedPrice == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : fixedPrice.setScale(2, RoundingMode.HALF_UP);
        grossMarginRate = grossMarginRate == null
                ? BigDecimal.ZERO
                : grossMarginRate;
    }
}
