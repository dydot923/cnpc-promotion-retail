package com.cnpc.promoretail.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventoryReplenishmentRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
        String operatorId,
        String note
) {

    public InventoryReplenishmentRequest {
        operatorId = operatorId == null || operatorId.isBlank() ? "stock-manager" : operatorId.trim();
        note = note == null ? "" : note.trim();
    }
}
