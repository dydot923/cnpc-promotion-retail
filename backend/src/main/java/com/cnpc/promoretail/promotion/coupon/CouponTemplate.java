package com.cnpc.promoretail.promotion.coupon;

import com.cnpc.promoretail.ruleengine.model.Coupon;
import com.cnpc.promoretail.ruleengine.model.CouponStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CouponTemplate(
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

    public CouponTemplate {
        if (couponTemplateId == null || couponTemplateId.isBlank()) {
            throw new IllegalArgumentException("couponTemplateId is required");
        }
        if (couponName == null || couponName.isBlank()) {
            throw new IllegalArgumentException("couponName is required");
        }
        faceValue = money(faceValue);
        minSpendAmount = money(minSpendAmount);
        applicableCategories = applicableCategories == null ? List.of() : List.copyOf(applicableCategories);
        excludedCategories = excludedCategories == null ? List.of() : List.copyOf(excludedCategories);
        applicableProductCodes = applicableProductCodes == null ? List.of() : List.copyOf(applicableProductCodes);
        excludedProductCodes = excludedProductCodes == null ? List.of() : List.copyOf(excludedProductCodes);
        validDays = Math.max(validDays, 0);
        issueQuantity = Math.max(issueQuantity, 0);
        perCustomerLimit = Math.max(perCustomerLimit, 0);
        redeemChannels = redeemChannels == null ? List.of() : List.copyOf(redeemChannels);
        discountRate = money(discountRate);
    }

    public CouponTemplate(
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
            boolean stackable
    ) {
        this(couponTemplateId, couponName, faceValue, minSpendAmount, applicableCategories, excludedCategories,
                applicableProductCodes, excludedProductCodes, validDays, issueQuantity, perCustomerLimit,
                redeemChannels, memberOnly, stackable, BigDecimal.ZERO);
    }

    public CouponTemplate withApplicableProductCodes(List<String> productCodes) {
        return new CouponTemplate(couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, productCodes, excludedProductCodes, validDays,
                issueQuantity, perCustomerLimit, redeemChannels, memberOnly, stackable, discountRate);
    }

    public Coupon toCoupon(String couponId, LocalDate validFrom, LocalDate validUntil, LocalDateTime issuedAt) {
        return new Coupon(couponId, couponTemplateId, couponName, faceValue, minSpendAmount,
                applicableCategories, excludedCategories, applicableProductCodes, excludedProductCodes,
                validFrom, validUntil, memberOnly, stackable, CouponStatus.AVAILABLE, issuedAt, null, "",
                discountRate);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
