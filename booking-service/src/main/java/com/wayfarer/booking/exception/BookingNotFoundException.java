package com.wayfarer.booking.exception;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(Long id) {
        super("No booking found with id: " + id);
    }
}
