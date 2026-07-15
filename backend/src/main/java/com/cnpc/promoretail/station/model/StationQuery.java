package com.cnpc.promoretail.station.model;

public record StationQuery(
        String city,
        String district,
        String stationType,
        String salesScope
) {

    public StationQuery {
        city = blankToNull(city);
        district = blankToNull(district);
        stationType = blankToNull(stationType);
        salesScope = blankToNull(salesScope);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
