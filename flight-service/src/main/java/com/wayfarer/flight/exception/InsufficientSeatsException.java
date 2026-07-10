package com.wayfarer.flight.exception;

public class InsufficientSeatsException extends RuntimeException {
    public InsufficientSeatsException(Long flightId, int requested, int available) {
        super("Flight " + flightId + " has only " + available + " seat(s) available, requested " + requested);
    }
}
