package com.wayfarer.booking.exception;

/**
 * Every resilience-wrapped downstream call (Feign failure, circuit breaker
 * OPEN, retries exhausted) collapses to this one exception type, so
 * BookingOrchestrationService's Saga failure/compensation logic doesn't need
 * to know or care which specific library/mechanism caused the failure.
 */
public class DownstreamCallException extends RuntimeException {
    public DownstreamCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
