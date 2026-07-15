package com.cnpc.promoretail.inventory.model;

import java.time.Instant;

public record InventoryAlertRecord(
        String alertId,
        String status,
        String handledBy,
        Instant handledAt,
        String handleNote,
        String replenishmentListId,
        Instant createdAt,
        Instant updatedAt
) {

    public InventoryAlertRecord {
        if (alertId == null || alertId.isBlank()) {
            throw new IllegalArgumentException("alertId is required");
        }
        status = status == null || status.isBlank() ? "OPEN" : status;
        handledBy = handledBy == null ? "" : handledBy;
        handleNote = handleNote == null ? "" : handleNote;
        replenishmentListId = replenishmentListId == null ? "" : replenishmentListId;
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static InventoryAlertRecord handled(
            String alertId,
            String operatorId,
            String note,
            InventoryAlertRecord previous,
            Instant now
    ) {
        return new InventoryAlertRecord(
                alertId,
                "HANDLED",
                operatorId == null || operatorId.isBlank() ? "system" : operatorId,
                now,
                note,
                previous == null ? "" : previous.replenishmentListId(),
                previous == null ? now : previous.createdAt(),
                now
        );
    }

    public static InventoryAlertRecord replenishmentCreated(
            String alertId,
            String replenishmentListId,
            String operatorId,
            InventoryAlertRecord previous,
            Instant now
    ) {
        return new InventoryAlertRecord(
                alertId,
                "REPLENISHMENT_CREATED",
                operatorId == null || operatorId.isBlank() ? "system" : operatorId,
                now,
                previous == null ? "" : previous.handleNote(),
                replenishmentListId,
                previous == null ? now : previous.createdAt(),
                now
        );
    }
}
