package com.wayfarer.booking.exception;

/** The Saga ran, at least one step failed, and compensation already ran — this reports that to the caller. */
public class BookingFailedException extends RuntimeException {
    public BookingFailedException(String reason) {
        super(reason);
    }
}
