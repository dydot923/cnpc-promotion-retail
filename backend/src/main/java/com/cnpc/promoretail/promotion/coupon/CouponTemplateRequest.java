package com.cnpc.promoretail.promotion.coupon;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CouponTemplateRequest(
        String couponTemplateId,
        @NotBlank String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        List<String> applicableProductCodes,
        List<String> excludedProductCodes,
        @Min(0) Integer validDays,
        @Min(0) Integer issueQuantity,
        @Min(0) Integer perCustomerLimit,
        List<String> redeemChannels,
        Boolean memberOnly,
        Boolean stackable,
        BigDecimal discountRate
) {

    public CouponTemplate toTemplate(String pathTemplateId) {
        String effectiveTemplateId = pathTemplateId == null || pathTemplateId.isBlank()
                ? requestTemplateId()
                : pathTemplateId.trim();
        return new CouponTemplate(
                effectiveTemplateId,
                couponName,
                faceValue,
                minSpendAmount,
                applicableCategories,
                excludedCategories,
                applicableProductCodes,
                excludedProductCodes,
                validDays == null ? 0 : validDays,
                issueQuantity == null ? 0 : issueQuantity,
                perCustomerLimit == null ? 0 : perCustomerLimit,
                redeemChannels,
                Boolean.TRUE.equals(memberOnly),
                Boolean.TRUE.equals(stackable),
                discountRate == null ? BigDecimal.ZERO : discountRate
        );
    }

    private String requestTemplateId() {
        return couponTemplateId == null || couponTemplateId.isBlank()
                ? "coupon-template-" + UUID.randomUUID()
                : couponTemplateId.trim();
    }
}
