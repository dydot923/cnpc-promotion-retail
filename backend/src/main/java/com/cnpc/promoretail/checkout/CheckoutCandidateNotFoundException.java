package com.cnpc.promoretail.checkout;

public class CheckoutCandidateNotFoundException extends RuntimeException {

    public CheckoutCandidateNotFoundException(String selectedCandidateId) {
        super("Checkout candidate not found: " + selectedCandidateId);
    }
}
