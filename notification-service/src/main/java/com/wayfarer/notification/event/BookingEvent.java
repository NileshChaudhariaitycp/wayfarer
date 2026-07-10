package com.wayfarer.notification.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * This service's own copy of the event contract booking-service publishes —
 * see ADR 0003/0006 for why there's no shared DTO module. Field names and
 * types must match booking-service's producer-side BookingEvent by
 * convention, not by compiler enforcement.
 */
public record BookingEvent(
        String eventType,
        Long bookingId,
        Long customerId,
        String bookingType,
        BigDecimal totalPrice,
        String failureReason,
        Instant timestamp
) {
}
