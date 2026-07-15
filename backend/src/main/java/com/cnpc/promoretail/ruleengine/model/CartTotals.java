package com.cnpc.promoretail.ruleengine.model;

import com.cnpc.promoretail.ruleengine.context.OrderContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record CartTotals(BigDecimal originalAmount) {

    public static CartTotals from(OrderContext context) {
        BigDecimal total = context.cartItems().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(context.fuel().amount())
                .add(context.rechargeAmount())
                .setScale(2, RoundingMode.HALF_UP);
        return new CartTotals(total);
    }
}
