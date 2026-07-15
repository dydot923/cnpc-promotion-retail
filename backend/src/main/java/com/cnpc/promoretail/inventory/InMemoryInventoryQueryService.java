package com.cnpc.promoretail.inventory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev-db & !postgres")
public class InMemoryInventoryQueryService implements InventoryQueryService {

    private final Map<String, BigDecimal> quantities = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getAvailableQuantity(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        return quantities.getOrDefault(productCode, new BigDecimal("999999"));
    }

    public void putAvailableQuantity(String productCode, BigDecimal quantity) {
        if (productCode != null && !productCode.isBlank()) {
            quantities.put(productCode, quantity == null ? BigDecimal.ZERO : quantity);
        }
    }
}
