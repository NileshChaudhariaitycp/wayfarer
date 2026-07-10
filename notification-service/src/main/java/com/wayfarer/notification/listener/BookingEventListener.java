package com.wayfarer.notification.listener;

import com.wayfarer.notification.event.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Mock notification "sending" — logs what a real system would email/SMS.
 * Runs independently of booking-service: if this service or Kafka is down,
 * bookings still succeed (see ADR 0006) — the notification just arrives
 * once both are healthy again, since Kafka retains unconsumed messages.
 */
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    @KafkaListener(topics = "booking-events", groupId = "notification-service")
    public void onBookingEvent(BookingEvent event) {
        switch (event.eventType()) {
            case "CONFIRMED" -> log.info(
                    "[MOCK EMAIL] To customer {}: Your booking {} ({}) is confirmed — total {}",
                    event.customerId(), event.bookingId(), event.bookingType(), event.totalPrice());
            case "FAILED" -> log.info(
                    "[MOCK EMAIL] To customer {}: Your booking {} could not be completed — {}",
                    event.customerId(), event.bookingId(), event.failureReason());
            case "CANCELLED" -> log.info(
                    "[MOCK EMAIL] To customer {}: Your booking {} has been cancelled and refunded",
                    event.customerId(), event.bookingId());
            default -> log.warn("Unrecognized booking event type: {}", event.eventType());
        }
    }
}
