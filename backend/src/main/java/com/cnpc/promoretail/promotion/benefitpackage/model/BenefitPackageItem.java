package com.cnpc.promoretail.promotion.benefitpackage.model;

import java.math.BigDecimal;

public record BenefitPackageItem(
        String itemName,
        BigDecimal quantity,
        String remark,
        Integer sourceRowNumber
) {

    public BenefitPackageItem {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("itemName is required");
        }
        quantity = quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE
                : quantity;
        remark = remark == null ? "" : remark;
    }
}
