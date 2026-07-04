package com.cnpc.promoretail.ruleengine.context;

public record StationContext(
        String stationId,
        String stationType,
        String region
) {

    public static StationContext defaultStation() {
        return new StationContext("default", "gas_station", null);
    }
}

