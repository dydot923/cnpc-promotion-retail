package com.cnpc.promoretail.checkout;

public class CheckoutTransactionAlreadyExistsException extends RuntimeException {

    public CheckoutTransactionAlreadyExistsException(String txnNo) {
        super("Checkout transaction already exists: " + txnNo);
    }
}
