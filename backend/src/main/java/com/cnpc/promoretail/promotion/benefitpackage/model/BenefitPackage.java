package com.cnpc.promoretail.promotion.benefitpackage.model;

import java.math.BigDecimal;
import java.util.List;

public record BenefitPackage(
        String packageCode,
        String packageName,
        String salesChannel,
        BigDecimal salePrice,
        String status,
        String sourceSheetName,
        Integer sourceRowNumber,
        List<BenefitPackageItem> items
) {

    public BenefitPackage {
        if (packageCode == null || packageCode.isBlank()) {
            throw new IllegalArgumentException("packageCode is required");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName is required");
        }
        salesChannel = salesChannel == null ? "" : salesChannel;
        salePrice = salePrice == null ? BigDecimal.ZERO : salePrice;
        status = status == null || status.isBlank() ? "ACTIVE" : status;
        sourceSheetName = sourceSheetName == null ? "" : sourceSheetName;
        items = items == null ? List.of() : List.copyOf(items);
    }

    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
