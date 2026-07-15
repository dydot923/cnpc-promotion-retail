package com.cnpc.promoretail.inventory;

import java.math.BigDecimal;

public interface InventoryQueryService {

    BigDecimal getAvailableQuantity(String productCode);

    default boolean hasEnough(String productCode, BigDecimal quantity) {
        BigDecimal required = quantity == null ? BigDecimal.ZERO : quantity;
        return getAvailableQuantity(productCode).compareTo(required) >= 0;
    }
}
