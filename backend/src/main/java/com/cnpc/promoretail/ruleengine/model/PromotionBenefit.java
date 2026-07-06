package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;

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
        BigDecimal bundlePrice
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
        giftCouponAmount = giftCouponAmount == null ? BigDecimal.ZERO : giftCouponAmount;
        bundlePrice = bundlePrice == null ? BigDecimal.ZERO : bundlePrice;
    }

    public static PromotionBenefit fixedPrice(BigDecimal fixedPrice) {
        return new PromotionBenefit(PromotionRuleType.FIXED_PRICE, fixedPrice, null, null, null,
                0, null, null, 0, null, null, null);
    }

    public static PromotionBenefit percentageDiscount(BigDecimal discountRate) {
        return new PromotionBenefit(PromotionRuleType.PERCENTAGE_DISCOUNT, null, discountRate, null, null,
                0, null, null, 0, null, null, null);
    }

    public static PromotionBenefit amountOff(BigDecimal amountOff) {
        return new PromotionBenefit(PromotionRuleType.AMOUNT_OFF, null, null, amountOff, null,
                0, null, null, 0, null, null, null);
    }

    public static PromotionBenefit exchangePurchase(BigDecimal exchangePrice) {
        return new PromotionBenefit(PromotionRuleType.EXCHANGE_PURCHASE, null, null, null, exchangePrice,
                0, null, null, 0, null, null, null);
    }

    public static PromotionBenefit exchangePurchase(BigDecimal exchangePrice, int exchangeQuantity) {
        return new PromotionBenefit(PromotionRuleType.EXCHANGE_PURCHASE, null, null, null, exchangePrice,
                exchangeQuantity, null, null, 0, null, null, null);
    }

    public static PromotionBenefit giftItem(String itemCode, String itemName, int quantity) {
        return new PromotionBenefit(PromotionRuleType.GIFT_ITEM, null, null, null, null,
                0, itemCode, itemName, quantity, null, null, null);
    }

    public static PromotionBenefit giftCoupon(String couponName, BigDecimal couponAmount) {
        return new PromotionBenefit(PromotionRuleType.GIFT_COUPON, null, null, null, null,
                0, null, null, 0, couponName, couponAmount, null);
    }

    public static PromotionBenefit bundlePrice(BigDecimal bundlePrice) {
        return new PromotionBenefit(PromotionRuleType.BUNDLE_PRICE, null, null, null, null,
                0, null, null, 0, null, null, bundlePrice);
    }

    public int applicableExchangeQuantity(int eligibleQuantity) {
        if (eligibleQuantity <= 0) {
            return 0;
        }
        return exchangeQuantity <= 0 ? eligibleQuantity : Math.min(exchangeQuantity, eligibleQuantity);
    }
}
