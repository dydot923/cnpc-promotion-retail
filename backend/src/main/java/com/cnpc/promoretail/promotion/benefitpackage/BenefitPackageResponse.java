package com.cnpc.promoretail.promotion.benefitpackage;

import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackage;
import com.cnpc.promoretail.promotion.benefitpackage.model.BenefitPackageItem;
import java.math.BigDecimal;
import java.util.List;

public record BenefitPackageResponse(
        String packageCode,
        String packageName,
        String salesChannel,
        BigDecimal salePrice,
        String status,
        String sourceSheetName,
        Integer sourceRowNumber,
        List<BenefitPackageItem> items
) {

    public static BenefitPackageResponse from(BenefitPackage benefitPackage) {
        return new BenefitPackageResponse(
                benefitPackage.packageCode(),
                benefitPackage.packageName(),
                benefitPackage.salesChannel(),
                benefitPackage.salePrice(),
                benefitPackage.status(),
                benefitPackage.sourceSheetName(),
                benefitPackage.sourceRowNumber(),
                benefitPackage.items()
        );
    }
}
