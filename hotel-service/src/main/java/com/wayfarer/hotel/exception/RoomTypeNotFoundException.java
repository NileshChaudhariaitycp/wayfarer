package com.wayfarer.hotel.exception;

public class RoomTypeNotFoundException extends RuntimeException {
    public RoomTypeNotFoundException(Long hotelId, Long roomTypeId) {
        super("No room type " + roomTypeId + " found for hotel " + hotelId);
    }
}
