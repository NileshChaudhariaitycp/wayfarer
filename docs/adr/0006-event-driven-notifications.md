# ADR 0006: Kafka events for notifications, alongside (not replacing) the Saga

**Status:** Accepted — 2026-07-10

## Context
booking-service's Saga (ADR 0005) is synchronous orchestration: reserve,
pay, earn points, confirm — each step waits for the previous one, because
the customer needs a definite yes/no answer before the request completes.
Sending a confirmation email/SMS is different: nobody should have to wait
for a notification to send before their booking finishes, and a
notification failing shouldn't fail the booking.

## Decision
booking-service publishes a `BookingEvent` (CONFIRMED/FAILED/CANCELLED) to a
Kafka topic (`booking-events`) after each outcome, then returns to the
caller immediately — it does not wait for or care whether anything consumes
that event. `notification-service` subscribes independently and logs a mock
"notification sent" message. This is a **hybrid** architecture: the
transactional core stays orchestration-based (certainty matters there); a
side-effect that can tolerate being asynchronous and eventually-consistent
gets an event instead.

Each service defines its own copy of the `BookingEvent` shape (same
no-shared-DTO-module reasoning as ADR 0003) — the producer disables Kafka's
type-header metadata (`spring.json.add.type.headers: false`) and the
consumer pins an explicit default deserialization type, rather than relying
on the producer's fully-qualified Java class name matching on the consumer
side (it can't — they're in different packages entirely).

## Alternatives considered
- **Synchronous call to notification-service**, same as payment/loyalty.
  Would work, but couples booking completion to notification-service being
  up and fast, for a step where neither actually matters to the customer.
- **Choreographed Saga** (services react to each other's events instead of
  booking-service calling them directly). Rejected for the *transactional*
  steps specifically — see ADR 0005's reasoning on needing certainty before
  confirming a booking. Adopted here only for the one step where
  eventual-consistency is actually fine.

## Consequences
- Kafka is now real infrastructure (a broker process), not just another JVM
  — needs to be running locally for this to work, and needs its own entry
  in docker-compose (Phase 7).
- If Kafka or notification-service is down, bookings still succeed —
  notifications are just delayed until both are healthy again (Kafka
  retains unconsumed messages). This is the point of decoupling it.
- notification-service reading a stale/incompatible event shape (e.g. after
  a field is renamed on the producer side without updating the consumer)
  fails silently at deserialization rather than at compile time — the
  trade-off of independently-versioned services instead of a shared DTO.
- **"Fire and forget" needed explicit tuning to actually be non-blocking**:
  found during implementation that `KafkaTemplate.send()` blocks the calling
  thread for up to `max.block.ms` (default 60s) to resolve topic metadata
  when the broker is unreachable — a real booking request hung for a full
  minute, then failed with a 500, before this was caught. Fixed by bounding
  `max.block.ms` to 3s and wrapping the `send()` call itself in try/catch
  (not just the returned future's `.whenComplete()`), since the initial call
  can throw synchronously too. See `BookingEventPublisher`'s javadoc.
- **Not live-verified end-to-end**: Docker Desktop failed to start on the
  development machine during this phase ("Docker Desktop is unable to
  start"), so there was no Kafka broker to run against. What *is* verified:
  both services boot cleanly with the Kafka client wired in, and bookings
  complete successfully even with the broker unreachable (graceful
  degradation, per the fix above). What's *not yet* verified: an actual
  event being produced and consumed — that a `CONFIRMED` booking makes a
  `[MOCK EMAIL]` line appear in notification-service's log. Do that the
  moment Docker/Kafka is available.
