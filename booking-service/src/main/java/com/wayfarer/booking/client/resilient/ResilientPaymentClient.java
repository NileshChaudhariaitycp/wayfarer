package com.wayfarer.booking.client.resilient;

import com.wayfarer.booking.client.PaymentServiceClient;
import com.wayfarer.booking.exception.DownstreamCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ResilientPaymentClient {

    private final PaymentServiceClient paymentServiceClient;

    public ResilientPaymentClient(PaymentServiceClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    // NOT idempotent — payment-service creates a brand new Payment row on
    // every call. Retrying a timed-out authorize risks double-charging.
    // Circuit breaker only.
    @CircuitBreaker(name = "paymentService", fallbackMethod = "authorizeFallback")
    public PaymentServiceClient.PaymentResult authorize(Long bookingId, BigDecimal amount, String cardToken) {
        return paymentServiceClient.authorize(new PaymentServiceClient.AuthorizeRequest(bookingId, amount, cardToken));
    }

    // Idempotent (payment-service checks for an already-REFUNDED payment and
    // no-ops) — safe to retry.
    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundFallback")
    @Retry(name = "paymentService")
    public PaymentServiceClient.PaymentResult refund(Long paymentId) {
        return paymentServiceClient.refund(paymentId);
    }

    @SuppressWarnings("unused")
    private PaymentServiceClient.PaymentResult authorizeFallback(Long bookingId, BigDecimal amount, String cardToken, Throwable t) {
        throw new DownstreamCallException("payment-service authorize failed for bookingId=" + bookingId, t);
    }

    @SuppressWarnings("unused")
    private PaymentServiceClient.PaymentResult refundFallback(Long paymentId, Throwable t) {
        throw new DownstreamCallException("payment-service refund failed for paymentId=" + paymentId, t);
    }
}
