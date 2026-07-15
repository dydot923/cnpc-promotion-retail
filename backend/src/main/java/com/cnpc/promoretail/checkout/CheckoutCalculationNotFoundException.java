package com.cnpc.promoretail.checkout;

public class CheckoutCalculationNotFoundException extends RuntimeException {

    public CheckoutCalculationNotFoundException(String calculationId) {
        super("Checkout calculation not found: " + calculationId);
    }
}
