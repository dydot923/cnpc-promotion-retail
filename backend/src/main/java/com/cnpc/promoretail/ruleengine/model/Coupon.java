package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Coupon(
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

    public Coupon {
        if (couponId == null || couponId.isBlank()) {
            throw new IllegalArgumentException("couponId is required");
        }
        couponTemplateId = couponTemplateId == null ? "" : couponTemplateId;
        couponName = couponName == null || couponName.isBlank() ? couponId : couponName;
        faceValue = money(faceValue);
        minSpendAmount = money(minSpendAmount);
        applicableCategories = applicableCategories == null ? List.of() : List.copyOf(applicableCategories);
        excludedCategories = excludedCategories == null ? List.of() : List.copyOf(excludedCategories);
        applicableProductCodes = applicableProductCodes == null ? List.of() : List.copyOf(applicableProductCodes);
        excludedProductCodes = excludedProductCodes == null ? List.of() : List.copyOf(excludedProductCodes);
        status = status == null ? CouponStatus.AVAILABLE : status;
        operatorId = operatorId == null ? "" : operatorId;
        discountRate = money(discountRate);
        sequenceGroup = sequenceGroup == null ? "" : sequenceGroup;
        holderMemberId = holderMemberId == null ? "" : holderMemberId;
        if (sequenceOrder != null && sequenceOrder < 1) {
            throw new IllegalArgumentException("sequenceOrder must be positive");
        }
    }

    public Coupon(
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
            Integer sequenceOrder
    ) {
        this(couponId, couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, memberOnly, stackable, status, issuedAt, usedAt, operatorId,
                discountRate, sequenceGroup, sequenceOrder, "");
    }

    public Coupon(
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
            BigDecimal discountRate
    ) {
        this(couponId, couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, memberOnly, stackable, status, issuedAt, usedAt, operatorId,
                discountRate, "", null, "");
    }

    public Coupon(
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
            String operatorId
    ) {
        this(couponId, couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, memberOnly, stackable, status, issuedAt, usedAt, operatorId,
                BigDecimal.ZERO, "", null, "");
    }

    public Coupon markUsed(LocalDateTime usedAt, String operatorId) {
        return new Coupon(
                couponId,
                couponTemplateId,
                couponName,
                faceValue,
                minSpendAmount,
                applicableCategories,
                excludedCategories,
                applicableProductCodes,
                excludedProductCodes,
                validFrom,
                validUntil,
                memberOnly,
                stackable,
                CouponStatus.USED,
                issuedAt,
                usedAt,
                operatorId,
                discountRate,
                sequenceGroup,
                sequenceOrder,
                holderMemberId
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
