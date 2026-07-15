package com.cnpc.promoretail.checkout;

import java.time.Instant;

public record CheckoutTransactionQuery(
        String memberCode,
        String stationCode,
        Instant startDate,
        Instant endDate,
        int limit
) {

    public CheckoutTransactionQuery {
        memberCode = memberCode == null ? "" : memberCode;
        stationCode = stationCode == null ? "" : stationCode;
        limit = limit <= 0 ? 50 : Math.min(limit, 200);
    }
}
