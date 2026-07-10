package com.wayfarer.booking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget by design (see ADR 0006): the caller doesn't wait for or
 * care whether this succeeds. Keyed by bookingId so all events for the same
 * booking land on the same Kafka partition, preserving their order.
 *
 * KafkaTemplate.send() is NOT purely asynchronous: if topic metadata isn't
 * already cached (unreachable broker, first message to a topic), the client
 * blocks the CALLING thread for up to producer.properties.max.block.ms
 * (bounded to 3s below — default is 60s) just to resolve partitioning,
 * before it even returns the CompletableFuture. That initial call can also
 * throw synchronously rather than failing the future, which is why this is
 * wrapped in a try/catch here in addition to the .whenComplete() below —
 * either failure path must stay non-fatal for the booking that triggered it.
 */
@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);
    private static final String TOPIC = "booking-events";

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    public BookingEventPublisher(KafkaTemplate<String, BookingEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(BookingEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.bookingId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {} event for booking {}: {}",
                                    event.eventType(), event.bookingId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("Failed to publish {} event for booking {}: {}", event.eventType(), event.bookingId(), ex.getMessage());
        }
    }
}
