package com.cnpc.promoretail.inventory;

public record InventoryAlertHandleRequest(
        String operatorId,
        String note
) {

    public InventoryAlertHandleRequest {
        operatorId = operatorId == null || operatorId.isBlank() ? "system" : operatorId;
        note = note == null ? "" : note;
    }
}
