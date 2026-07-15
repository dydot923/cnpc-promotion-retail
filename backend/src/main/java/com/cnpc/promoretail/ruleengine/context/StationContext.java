package com.cnpc.promoretail.ruleengine.context;

public record StationContext(
        String stationId,
        String stationType,
        String province,
        String city,
        String district
) {

    public StationContext(String stationId, String stationType, String region) {
        this(stationId, stationType, region, null, null);
    }

    public StationContext {
        stationId = stationId == null || stationId.isBlank() ? "default" : stationId.trim();
        stationType = stationType == null || stationType.isBlank() ? "gas_station" : stationType.trim();
        province = province == null || province.isBlank() ? null : province.trim();
        city = city == null || city.isBlank() ? null : city.trim();
        district = district == null || district.isBlank() ? null : district.trim();
    }

    public static StationContext defaultStation() {
        return new StationContext("default", "gas_station", "\u65b0\u7586", null, null);
    }

    public String region() {
        return province;
    }
}
