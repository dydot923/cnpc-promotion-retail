package com.cnpc.promoretail.replenishment.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReplenishmentItem(
        String productCode,
        String barcode,
        String productName,
        String category,
        BigDecimal currentQuantity,
        BigDecimal threshold,
        BigDecimal suggestedQuantity,
        String relatedPromotion,
        String reason
) {

    public ReplenishmentItem {
        currentQuantity = quantity(currentQuantity);
        threshold = quantity(threshold);
        suggestedQuantity = quantity(suggestedQuantity);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
