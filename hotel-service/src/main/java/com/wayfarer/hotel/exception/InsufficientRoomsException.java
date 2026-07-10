package com.wayfarer.hotel.exception;

public class InsufficientRoomsException extends RuntimeException {
    public InsufficientRoomsException(Long roomTypeId, int requested, int available) {
        super("Room type " + roomTypeId + " has only " + available + " room(s) available, requested " + requested);
    }
}
