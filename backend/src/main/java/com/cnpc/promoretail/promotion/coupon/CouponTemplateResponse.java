package com.cnpc.promoretail.promotion.coupon;

import java.math.BigDecimal;
import java.util.List;

public record CouponTemplateResponse(
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        List<String> applicableProductCodes,
        List<String> excludedProductCodes,
        int validDays,
        int issueQuantity,
        int perCustomerLimit,
        List<String> redeemChannels,
        boolean memberOnly,
        boolean stackable,
        BigDecimal discountRate
) {

    public static CouponTemplateResponse from(CouponTemplate template) {
        return new CouponTemplateResponse(
                template.couponTemplateId(),
                template.couponName(),
                template.faceValue(),
                template.minSpendAmount(),
                template.applicableCategories(),
                template.excludedCategories(),
                template.applicableProductCodes(),
                template.excludedProductCodes(),
                template.validDays(),
                template.issueQuantity(),
                template.perCustomerLimit(),
                template.redeemChannels(),
                template.memberOnly(),
                template.stackable(),
                template.discountRate()
        );
    }
}
