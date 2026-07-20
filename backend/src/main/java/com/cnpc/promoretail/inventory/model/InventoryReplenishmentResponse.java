package com.cnpc.promoretail.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record InventoryReplenishmentResponse(
        String operationId,
        String productCode,
        String productName,
        BigDecimal quantityBefore,
        BigDecimal replenishedQuantity,
        BigDecimal quantityAfter,
        String operatorId,
        String note,
        Instant replenishedAt
) {

    public InventoryReplenishmentResponse {
        quantityBefore = quantity(quantityBefore);
        replenishedQuantity = quantity(replenishedQuantity);
        quantityAfter = quantity(quantityAfter);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
