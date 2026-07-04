package com.cnpc.promoretail.ruleengine.model;

import java.math.BigDecimal;

public record RuleBenefit(
        PromotionRuleType type,
        BigDecimal fixedPrice,
        BigDecimal discountRate,
        BigDecimal amountOff,
        BigDecimal exchangePrice,
        String giftItemCode,
        String giftItemName,
        int giftItemQuantity,
        String giftCouponName,
        BigDecimal giftCouponAmount,
        BigDecimal bundlePrice
) {

    public RuleBenefit {
        if (type == null) {
            throw new IllegalArgumentException("benefit type is required");
        }
        fixedPrice = fixedPrice == null ? BigDecimal.ZERO : fixedPrice;
        discountRate = discountRate == null ? BigDecimal.ZERO : discountRate;
        amountOff = amountOff == null ? BigDecimal.ZERO : amountOff;
        exchangePrice = exchangePrice == null ? BigDecimal.ZERO : exchangePrice;
        giftCouponAmount = giftCouponAmount == null ? BigDecimal.ZERO : giftCouponAmount;
        bundlePrice = bundlePrice == null ? BigDecimal.ZERO : bundlePrice;
    }

    public static RuleBenefit fixedPrice(BigDecimal fixedPrice) {
        return new RuleBenefit(PromotionRuleType.FIXED_PRICE, fixedPrice, null, null, null,
                null, null, 0, null, null, null);
    }

    public static RuleBenefit percentageDiscount(BigDecimal discountRate) {
        return new RuleBenefit(PromotionRuleType.PERCENTAGE_DISCOUNT, null, discountRate, null, null,
                null, null, 0, null, null, null);
    }

    public static RuleBenefit amountOff(BigDecimal amountOff) {
        return new RuleBenefit(PromotionRuleType.AMOUNT_OFF, null, null, amountOff, null,
                null, null, 0, null, null, null);
    }

    public static RuleBenefit exchangePurchase(BigDecimal exchangePrice) {
        return new RuleBenefit(PromotionRuleType.EXCHANGE_PURCHASE, null, null, null, exchangePrice,
                null, null, 0, null, null, null);
    }

    public static RuleBenefit giftItem(String itemCode, String itemName, int quantity) {
        return new RuleBenefit(PromotionRuleType.GIFT_ITEM, null, null, null, null,
                itemCode, itemName, quantity, null, null, null);
    }

    public static RuleBenefit giftCoupon(String couponName, BigDecimal couponAmount) {
        return new RuleBenefit(PromotionRuleType.GIFT_COUPON, null, null, null, null,
                null, null, 0, couponName, couponAmount, null);
    }

    public static RuleBenefit bundlePrice(BigDecimal bundlePrice) {
        return new RuleBenefit(PromotionRuleType.BUNDLE_PRICE, null, null, null, null,
                null, null, 0, null, null, bundlePrice);
    }
}

