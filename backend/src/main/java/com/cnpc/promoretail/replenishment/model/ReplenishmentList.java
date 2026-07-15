package com.cnpc.promoretail.replenishment.model;

import java.time.Instant;
import java.util.List;

public record ReplenishmentList(
        String listId,
        String listName,
        String status,
        List<ReplenishmentItem> items,
        int totalItems,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
) {

    public ReplenishmentList {
        if (listId == null || listId.isBlank()) {
            throw new IllegalArgumentException("listId is required");
        }
        listName = listName == null || listName.isBlank() ? listId : listName;
        status = status == null || status.isBlank() ? "DRAFT" : status;
        items = items == null ? List.of() : List.copyOf(items);
        totalItems = totalItems <= 0 ? items.size() : totalItems;
        createdBy = createdBy == null || createdBy.isBlank() ? "system" : createdBy;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedBy = updatedBy == null || updatedBy.isBlank() ? createdBy : updatedBy;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public ReplenishmentList(String listId, Instant createdAt, String status, List<ReplenishmentItem> items) {
        this(listId, listId, status, items, items == null ? 0 : items.size(), "system", createdAt, "system", createdAt);
    }

    public ReplenishmentList withStatus(String newStatus, String operatorId, Instant now) {
        return new ReplenishmentList(listId, listName, newStatus, items, totalItems,
                createdBy, createdAt, operatorId, now);
    }
}
