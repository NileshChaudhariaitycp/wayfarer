package com.wayfarer.hotel.exception;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(Long id) {
        super("No hotel found with id: " + id);
    }
}
