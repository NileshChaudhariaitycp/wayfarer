package com.wayfarer.booking.client.resilient;

import com.wayfarer.booking.client.LoyaltyServiceClient;
import com.wayfarer.booking.exception.DownstreamCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/** Both operations are idempotent (dedup by bookingId in loyalty-service) — both are safe to retry. */
@Component
public class ResilientLoyaltyClient {

    private final LoyaltyServiceClient loyaltyServiceClient;

    public ResilientLoyaltyClient(LoyaltyServiceClient loyaltyServiceClient) {
        this.loyaltyServiceClient = loyaltyServiceClient;
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "earnFallback")
    @Retry(name = "loyaltyService")
    public void earn(Long userId, Long bookingId, int points) {
        loyaltyServiceClient.earn(userId, new LoyaltyServiceClient.EarnRequest(bookingId, points));
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "reverseFallback")
    @Retry(name = "loyaltyService")
    public void reverse(Long userId, Long bookingId) {
        loyaltyServiceClient.reverse(userId, bookingId);
    }

    @SuppressWarnings("unused")
    private void earnFallback(Long userId, Long bookingId, int points, Throwable t) {
        throw new DownstreamCallException("loyalty-service earn failed for userId=" + userId + " bookingId=" + bookingId, t);
    }

    @SuppressWarnings("unused")
    private void reverseFallback(Long userId, Long bookingId, Throwable t) {
        throw new DownstreamCallException("loyalty-service reverse failed for userId=" + userId + " bookingId=" + bookingId, t);
    }
}
