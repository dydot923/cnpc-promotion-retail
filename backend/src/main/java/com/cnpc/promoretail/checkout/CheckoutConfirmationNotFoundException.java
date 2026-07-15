package com.cnpc.promoretail.checkout;

public class CheckoutConfirmationNotFoundException extends RuntimeException {

    public CheckoutConfirmationNotFoundException(String confirmationId) {
        super("Checkout confirmation not found: " + confirmationId);
    }
}
