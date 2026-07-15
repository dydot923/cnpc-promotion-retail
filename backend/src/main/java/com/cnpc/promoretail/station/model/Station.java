package com.cnpc.promoretail.station.model;

import java.math.BigDecimal;
import java.util.List;

public record Station(
        String stationCode,
        String hosCode,
        String stationName,
        String branchCompany,
        String prefecture,
        String province,
        String city,
        String district,
        String address,
        BigDecimal longitude,
        BigDecimal latitude,
        String contactName,
        String contactPhone,
        String stationType,
        List<String> salesScope,
        String remark,
        String sourceSheetName,
        Integer sourceRowNumber,
        boolean demoData
) {

    public Station {
        if (stationCode == null || stationCode.isBlank()) {
            throw new IllegalArgumentException("stationCode is required");
        }
        if (stationName == null || stationName.isBlank()) {
            throw new IllegalArgumentException("stationName is required");
        }
        hosCode = blankToEmpty(hosCode);
        branchCompany = blankToEmpty(branchCompany);
        prefecture = blankToEmpty(prefecture);
        province = blankToDefault(province, "\u65b0\u7586");
        city = blankToEmpty(city);
        district = blankToEmpty(district);
        address = blankToEmpty(address);
        contactName = blankToEmpty(contactName);
        contactPhone = blankToEmpty(contactPhone);
        stationType = blankToDefault(stationType, "gas_station");
        salesScope = salesScope == null ? List.of() : List.copyOf(salesScope);
        remark = blankToEmpty(remark);
        sourceSheetName = blankToEmpty(sourceSheetName);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
