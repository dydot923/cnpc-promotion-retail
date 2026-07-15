package com.cnpc.promoretail.checkout;

public class CheckoutTransactionNotFoundException extends RuntimeException {

    public CheckoutTransactionNotFoundException(String txnNo) {
        super("Checkout transaction not found: " + txnNo);
    }
}
