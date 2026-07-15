package com.cnpc.promoretail.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CheckoutTransactionItem(
        String productCode,
        String productName,
        String barcode,
        String category,
        BigDecimal unitPrice,
        BigDecimal actualPrice,
        int quantity,
        BigDecimal subtotal,
        String appliedPromoId,
        String appliedCouponCode
) {

    public CheckoutTransactionItem {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode is required");
        }
        productName = productName == null ? "" : productName;
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        unitPrice = money(unitPrice);
        actualPrice = money(actualPrice == null ? unitPrice : actualPrice);
        subtotal = money(subtotal == null
                ? actualPrice.multiply(BigDecimal.valueOf(quantity))
                : subtotal);
        appliedPromoId = blankToNull(appliedPromoId);
        appliedCouponCode = blankToNull(appliedCouponCode);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
