package com.cnpc.promoretail.ruleengine.context;

import java.util.List;

public record CustomerContext(
        boolean member,
        String memberLevel,
        List<String> availableCouponIds
) {

    public CustomerContext {
        availableCouponIds = availableCouponIds == null ? List.of() : List.copyOf(availableCouponIds);
    }

    public static CustomerContext anonymous() {
        return new CustomerContext(false, null, List.of());
    }
}

