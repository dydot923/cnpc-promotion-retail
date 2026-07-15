package com.cnpc.promoretail.member;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberCouponResponse(
        String couponId,
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        String status,
        LocalDate validFrom,
        LocalDate validUntil,
        List<String> applicableCategories,
        List<String> excludedCategories,
        List<String> applicableProductCodes
) {

    public static MemberCouponResponse from(Coupon coupon) {
        return new MemberCouponResponse(
                coupon.couponId(),
                coupon.couponTemplateId(),
                coupon.couponName(),
                coupon.faceValue(),
                coupon.minSpendAmount(),
                coupon.status().name(),
                coupon.validFrom(),
                coupon.validUntil(),
                coupon.applicableCategories(),
                coupon.excludedCategories(),
                coupon.applicableProductCodes()
        );
    }
}
