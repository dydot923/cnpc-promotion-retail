package com.cnpc.promoretail.checkout;

public class CheckoutCalculationAlreadyConfirmedException extends RuntimeException {

    public CheckoutCalculationAlreadyConfirmedException(String calculationId) {
        super("Checkout calculation already confirmed: " + calculationId);
    }
}
