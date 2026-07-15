package com.cnpc.promoretail.replenishment;

public class ReplenishmentListNotFoundException extends RuntimeException {

    public ReplenishmentListNotFoundException(String listId) {
        super("未找到补货清单: " + listId);
    }
}
