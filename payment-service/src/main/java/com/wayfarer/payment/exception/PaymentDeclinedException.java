package com.wayfarer.payment.exception;

public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException() {
        super("Payment declined");
    }
}
