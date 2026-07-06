package com.cnpc.promoretail.checkout.model;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import java.time.Instant;
import java.util.List;

public record CheckoutCalculationRecord(
        String calculationId,
        OrderContext requestSnapshot,
        CalculationResult resultSnapshot,
        List<String> ruleVersionIds,
        Instant createdAt
) {

    public CheckoutCalculationRecord {
        if (calculationId == null || calculationId.isBlank()) {
            throw new IllegalArgumentException("calculationId is required");
        }
        if (requestSnapshot == null) {
            throw new IllegalArgumentException("requestSnapshot is required");
        }
        if (resultSnapshot == null) {
            throw new IllegalArgumentException("resultSnapshot is required");
        }
        ruleVersionIds = ruleVersionIds == null ? List.of() : List.copyOf(ruleVersionIds);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
