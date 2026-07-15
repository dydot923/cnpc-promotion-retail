package com.cnpc.promoretail.station.model;

import java.math.BigDecimal;
import java.util.List;

public record StationResponse(
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

    public static StationResponse from(Station station) {
        return new StationResponse(
                station.stationCode(),
                station.hosCode(),
                station.stationName(),
                station.branchCompany(),
                station.prefecture(),
                station.province(),
                station.city(),
                station.district(),
                station.address(),
                station.longitude(),
                station.latitude(),
                station.contactName(),
                station.contactPhone(),
                station.stationType(),
                station.salesScope(),
                station.remark(),
                station.sourceSheetName(),
                station.sourceRowNumber(),
                station.demoData()
        );
    }
}
