package com.wayfarer.booking.client.resilient;

import com.wayfarer.booking.client.FlightServiceClient;
import com.wayfarer.booking.exception.DownstreamCallException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

/**
 * Wraps the raw Feign client with resilience patterns, and is what
 * BookingOrchestrationService actually depends on. The split by method
 * matters: getFlight is read-only and safe to retry automatically;
 * reserveSeats/releaseSeats are NOT idempotent (see flight-service's own
 * comments on those endpoints) — retrying a timed-out call risks acting
 * twice on a request that actually succeeded server-side. Those get a
 * circuit breaker (fail fast after repeated failures) but never automatic
 * retry.
 */
@Component
public class ResilientFlightClient {

    private final FlightServiceClient flightServiceClient;

    public ResilientFlightClient(FlightServiceClient flightServiceClient) {
        this.flightServiceClient = flightServiceClient;
    }

    @CircuitBreaker(name = "flightService", fallbackMethod = "getFlightFallback")
    @Retry(name = "flightService")
    public FlightServiceClient.FlightDetails getFlight(Long id) {
        return flightServiceClient.getFlight(id);
    }

    @CircuitBreaker(name = "flightService", fallbackMethod = "reserveSeatsFallback")
    public void reserveSeats(Long id, int seats) {
        flightServiceClient.reserveSeats(id, new FlightServiceClient.SeatsRequest(seats));
    }

    @CircuitBreaker(name = "flightService", fallbackMethod = "releaseSeatsFallback")
    public void releaseSeats(Long id, int seats) {
        flightServiceClient.releaseSeats(id, new FlightServiceClient.SeatsRequest(seats));
    }

    @SuppressWarnings("unused")
    private FlightServiceClient.FlightDetails getFlightFallback(Long id, Throwable t) {
        throw new DownstreamCallException("flight-service getFlight failed for flightId=" + id, t);
    }

    @SuppressWarnings("unused")
    private void reserveSeatsFallback(Long id, int seats, Throwable t) {
        throw new DownstreamCallException("flight-service reserveSeats failed for flightId=" + id, t);
    }

    @SuppressWarnings("unused")
    private void releaseSeatsFallback(Long id, int seats, Throwable t) {
        throw new DownstreamCallException("flight-service releaseSeats failed for flightId=" + id, t);
    }
}
