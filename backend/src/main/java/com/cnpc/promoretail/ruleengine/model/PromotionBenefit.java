package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;
import java.util.List;

public record PromotionBenefit(
        PromotionRuleType type,
        BigDecimal fixedPrice,
        BigDecimal discountRate,
        BigDecimal amountOff,
        BigDecimal exchangePrice,
        int exchangeQuantity,
        String giftItemCode,
        String giftItemName,
        int giftItemQuantity,
        String giftCouponName,
        BigDecimal giftCouponAmount,
        int giftCouponQuantity,
        BigDecimal giftCouponUseThreshold,
        int giftCouponValidDays,
        String bundleId,
        List<BundleItem> bundleItems,
        BigDecimal bundlePrice,
        BigDecimal discountPerUnit,
        List<GiftCouponTier> giftCouponTiers,
        List<List<GiftItem>> giftItemOptions,
        List<CompositeBenefitComponent> compositeComponents,
        int pointsMultiplier
) {

    public PromotionBenefit {
        if (type == null) {
            throw new IllegalArgumentException("benefit type is required");
        }
        fixedPrice = fixedPrice == null ? BigDecimal.ZERO : fixedPrice;
        discountRate = discountRate == null ? BigDecimal.ZERO : discountRate;
        amountOff = amountOff == null ? BigDecimal.ZERO : amountOff;
        exchangePrice = exchangePrice == null ? BigDecimal.ZERO : exchangePrice;
        exchangeQuantity = Math.max(exchangeQuantity, 0);
        giftItemQuantity = Math.max(giftItemQuantity, 0);
        giftCouponAmount = giftCouponAmount == null ? BigDecimal.ZERO : giftCouponAmount;
        giftCouponQuantity = giftCouponQuantity <= 0 ? 1 : giftCouponQuantity;
        giftCouponUseThreshold = giftCouponUseThreshold == null ? BigDecimal.ZERO : giftCouponUseThreshold;
        giftCouponValidDays = Math.max(giftCouponValidDays, 0);
        bundleId = bundleId == null ? "" : bundleId;
        bundleItems = bundleItems == null ? List.of() : List.copyOf(bundleItems);
        bundlePrice = bundlePrice == null ? BigDecimal.ZERO : bundlePrice;
        discountPerUnit = discountPerUnit == null ? BigDecimal.ZERO : discountPerUnit;
        giftCouponTiers = giftCouponTiers == null ? List.of() : List.copyOf(giftCouponTiers);
        giftItemOptions = giftItemOptions == null ? List.of()
                : giftItemOptions.stream().map(List::copyOf).toList();
        compositeComponents = compositeComponents == null ? List.of() : List.copyOf(compositeComponents);
        pointsMultiplier = pointsMultiplier <= 0 ? 1 : pointsMultiplier;
    }

    public PromotionBenefit(
            PromotionRuleType type,
            BigDecimal fixedPrice,
            BigDecimal discountRate,
            BigDecimal amountOff,
            BigDecimal exchangePrice,
            int exchangeQuantity,
            String giftItemCode,
            String giftItemName,
            int giftItemQuantity,
            String giftCouponName,
            BigDecimal giftCouponAmount,
            int giftCouponQuantity,
            BigDecimal giftCouponUseThreshold,
            int giftCouponValidDays,
            String bundleId,
            List<BundleItem> bundleItems,
            BigDecimal bundlePrice,
            BigDecimal discountPerUnit,
            List<GiftCouponTier> giftCouponTiers,
            List<List<GiftItem>> giftItemOptions,
            List<CompositeBenefitComponent> compositeComponents
    ) {
        this(type, fixedPrice, discountRate, amountOff, exchangePrice, exchangeQuantity, giftItemCode,
                giftItemName, giftItemQuantity, giftCouponName, giftCouponAmount, giftCouponQuantity,
                giftCouponUseThreshold, giftCouponValidDays, bundleId, bundleItems, bundlePrice,
                discountPerUnit, giftCouponTiers, giftItemOptions, compositeComponents, 1);
    }

    public static PromotionBenefit fixedPrice(BigDecimal fixedPrice) {
        return new PromotionBenefit(PromotionRuleType.FIXED_PRICE, fixedPrice, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit percentageDiscount(BigDecimal discountRate) {
        return new PromotionBenefit(PromotionRuleType.PERCENTAGE_DISCOUNT, null, discountRate, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit percentageDiscount(BigDecimal discountRate, int pointsMultiplier) {
        return new PromotionBenefit(PromotionRuleType.PERCENTAGE_DISCOUNT, null, discountRate, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null,
                pointsMultiplier);
    }

    public static PromotionBenefit amountOff(BigDecimal amountOff) {
        return new PromotionBenefit(PromotionRuleType.AMOUNT_OFF, null, null, amountOff, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit exchangePurchase(BigDecimal exchangePrice) {
        return new PromotionBenefit(PromotionRuleType.EXCHANGE_PURCHASE, null, null, null, exchangePrice,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit exchangePurchase(BigDecimal exchangePrice, int exchangeQuantity) {
        return new PromotionBenefit(PromotionRuleType.EXCHANGE_PURCHASE, null, null, null, exchangePrice,
                exchangeQuantity, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit giftItem(String itemCode, String itemName, int quantity) {
        return new PromotionBenefit(PromotionRuleType.GIFT_ITEM, null, null, null, null,
                0, itemCode, itemName, quantity, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit giftItemOptions(List<List<GiftItem>> options) {
        return new PromotionBenefit(PromotionRuleType.GIFT_ITEM, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, options, null);
    }

    public static PromotionBenefit giftCoupon(String couponName, BigDecimal couponAmount) {
        return giftCoupon(couponName, couponAmount, 1, BigDecimal.ZERO, 0);
    }

    public static PromotionBenefit giftCoupon(
            String couponName,
            BigDecimal couponAmount,
            int quantity,
            BigDecimal useThreshold,
            int validDays
    ) {
        return new PromotionBenefit(PromotionRuleType.GIFT_COUPON, null, null, null, null,
                0, null, null, 0, couponName, couponAmount, quantity, useThreshold, validDays, null, null, null,
                null, null, null, null);
    }

    public static PromotionBenefit tieredGiftCoupons(List<GiftCouponTier> giftCouponTiers) {
        return new PromotionBenefit(PromotionRuleType.GIFT_COUPON, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, giftCouponTiers, null, null);
    }

    public static PromotionBenefit bundlePrice(BigDecimal bundlePrice) {
        return bundlePrice("", List.of(), bundlePrice);
    }

    public static PromotionBenefit bundlePrice(String bundleId, List<BundleItem> bundleItems, BigDecimal bundlePrice) {
        return new PromotionBenefit(PromotionRuleType.BUNDLE_PRICE, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, bundleId, bundleItems, bundlePrice, null, null, null, null);
    }

    public static PromotionBenefit couponRedeem() {
        return new PromotionBenefit(PromotionRuleType.COUPON_REDEEM, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, null);
    }

    public static PromotionBenefit fuelVolumeDiscount(BigDecimal discountPerUnit) {
        return new PromotionBenefit(PromotionRuleType.FUEL_VOLUME_DISCOUNT, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, discountPerUnit, null, null, null);
    }

    public static PromotionBenefit composite(List<CompositeBenefitComponent> components) {
        return new PromotionBenefit(PromotionRuleType.COMPOSITE, null, null, null, null,
                0, null, null, 0, null, null, 0, null, 0, null, null, null, null, null, null, components);
    }

    public int applicableExchangeQuantity(int eligibleQuantity) {
        if (eligibleQuantity <= 0) {
            return 0;
        }
        return exchangeQuantity <= 0 ? eligibleQuantity : Math.min(exchangeQuantity, eligibleQuantity);
    }
}
