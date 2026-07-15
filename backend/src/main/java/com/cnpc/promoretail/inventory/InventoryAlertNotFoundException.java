package com.cnpc.promoretail.inventory;

public class InventoryAlertNotFoundException extends RuntimeException {

    public InventoryAlertNotFoundException(String alertId) {
        super("Inventory alert not found: " + alertId);
    }
}
