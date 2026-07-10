package com.wayfarer.booking.event;

import java.math.BigDecimal;
import java.time.Instant;

/** See ADR 0006 — this service's producer-side copy of the event contract notification-service consumes. */
public record BookingEvent(
        String eventType,
        Long bookingId,
        Long customerId,
        String bookingType,
        BigDecimal totalPrice,
        String failureReason,
        Instant timestamp
) {
    public static BookingEvent confirmed(Long bookingId, Long customerId, String bookingType, BigDecimal totalPrice) {
        return new BookingEvent("CONFIRMED", bookingId, customerId, bookingType, totalPrice, null, Instant.now());
    }

    public static BookingEvent failed(Long bookingId, Long customerId, String bookingType, String failureReason) {
        return new BookingEvent("FAILED", bookingId, customerId, bookingType, null, failureReason, Instant.now());
    }

    public static BookingEvent cancelled(Long bookingId, Long customerId, String bookingType) {
        return new BookingEvent("CANCELLED", bookingId, customerId, bookingType, null, null, Instant.now());
    }
}
