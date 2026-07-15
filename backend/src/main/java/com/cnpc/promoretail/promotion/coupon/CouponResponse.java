package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CouponResponse(
        String couponId,
        String couponTemplateId,
        String couponName,
        BigDecimal faceValue,
        BigDecimal minSpendAmount,
        List<String> applicableCategories,
        List<String> excludedCategories,
        List<String> applicableProductCodes,
        List<String> excludedProductCodes,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean memberOnly,
        boolean stackable,
        CouponStatus status,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        String operatorId,
        BigDecimal discountRate,
        String sequenceGroup,
        Integer sequenceOrder,
        String holderMemberId
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.couponId(),
                coupon.couponTemplateId(),
                coupon.couponName(),
                coupon.faceValue(),
                coupon.minSpendAmount(),
                coupon.applicableCategories(),
                coupon.excludedCategories(),
                coupon.applicableProductCodes(),
                coupon.excludedProductCodes(),
                coupon.validFrom(),
                coupon.validUntil(),
                coupon.memberOnly(),
                coupon.stackable(),
                coupon.status(),
                coupon.issuedAt(),
                coupon.usedAt(),
                coupon.operatorId(),
                coupon.discountRate(),
                coupon.sequenceGroup(),
                coupon.sequenceOrder(),
                coupon.holderMemberId()
        );
    }
}
