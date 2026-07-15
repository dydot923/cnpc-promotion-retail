package com.cnpc.promoretail.promotion.benefitpackage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record BenefitPackagePurchaseRequest(
        @NotBlank String memberCode,
        String stationCode,
        @DecimalMin("0.00") BigDecimal paymentAmount,
        String checkoutTransactionNo,
        String operatorId,
        String operatorName
) {
}
