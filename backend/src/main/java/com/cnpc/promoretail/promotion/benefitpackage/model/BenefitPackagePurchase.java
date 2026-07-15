package com.cnpc.promoretail.promotion.benefitpackage.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BenefitPackagePurchase(
        String purchaseId,
        String memberCode,
        String packageCode,
        String packageName,
        BigDecimal salePrice,
        BigDecimal paymentAmount,
        String stationCode,
        String checkoutTransactionNo,
        String status,
        List<BenefitPackageItem> entitlementSnapshot,
        Instant purchasedAt,
        Instant activatedAt,
        Instant expiredAt,
        String operatorId,
        String operatorName
) {

    public BenefitPackagePurchase {
        if (purchaseId == null || purchaseId.isBlank()) {
            throw new IllegalArgumentException("purchaseId is required");
        }
        if (memberCode == null || memberCode.isBlank()) {
            throw new IllegalArgumentException("memberCode is required");
        }
        if (packageCode == null || packageCode.isBlank()) {
            throw new IllegalArgumentException("packageCode is required");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName is required");
        }
        salePrice = salePrice == null ? BigDecimal.ZERO : salePrice;
        paymentAmount = paymentAmount == null ? salePrice : paymentAmount;
        stationCode = stationCode == null ? "" : stationCode;
        checkoutTransactionNo = checkoutTransactionNo == null ? "" : checkoutTransactionNo;
        status = status == null || status.isBlank() ? "PURCHASED" : status;
        entitlementSnapshot = entitlementSnapshot == null ? List.of() : List.copyOf(entitlementSnapshot);
        purchasedAt = purchasedAt == null ? Instant.now() : purchasedAt;
        operatorId = operatorId == null ? "" : operatorId;
        operatorName = operatorName == null ? "" : operatorName;
    }
}
