package com.wayfarer.flight.exception;

public class FlightNotFoundException extends RuntimeException {
    public FlightNotFoundException(Long id) {
        super("No flight found with id: " + id);
    }
}
