package com.cnpc.promoretail.checkout;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutCalculateRequest(
        @Valid @NotNull OrderContext orderContext
) {
}

