package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackagePurchase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BenefitPackagePurchaseResponse(
        String purchaseId,
        String packageCode,
        String packageName,
        BigDecimal salePrice,
        BigDecimal paymentAmount,
        String memberCode,
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

    public static BenefitPackagePurchaseResponse from(BenefitPackagePurchase purchase) {
        return new BenefitPackagePurchaseResponse(
                purchase.purchaseId(),
                purchase.packageCode(),
                purchase.packageName(),
                purchase.salePrice(),
                purchase.paymentAmount(),
                purchase.memberCode(),
                purchase.stationCode(),
                purchase.checkoutTransactionNo(),
                purchase.status(),
                purchase.entitlementSnapshot(),
                purchase.purchasedAt(),
                purchase.activatedAt(),
                purchase.expiredAt(),
                purchase.operatorId(),
                purchase.operatorName()
        );
    }
}
