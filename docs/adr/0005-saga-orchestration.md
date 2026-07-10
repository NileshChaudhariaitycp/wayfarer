# ADR 0005: Orchestration-based Saga in booking-service, pessimistic locking in inventory services

**Status:** Accepted — 2026-07-10

## Context
A single booking spans four services with four separate databases: reserve
inventory (flight-service or hotel-service), authorize payment
(payment-service), award loyalty points (loyalty-service), then confirm.
There is no database transaction that can span all four. If step 3 fails
after steps 1–2 succeeded, the system is left in a partial state unless
something explicitly undoes the completed steps.

Seat/room counts are also a contended resource: many customers can try to
book the same flight's last seat at once, and exactly one must win.

## Decision
- **booking-service is a Saga orchestrator.** It calls each participant
  (flight/hotel → payment → loyalty) in sequence via OpenFeign. On failure at
  any step, it calls the compensating action for every step that already
  succeeded, in reverse order (refund payment, release seats/rooms), then
  marks the booking FAILED. This is *orchestration*, not *choreography* —
  one coordinator explicitly directs every step, rather than services
  reacting to each other's published events. Choreography needs an event
  bus to react to; we don't have one until Phase 5's Kafka, so orchestration
  is the only option available now, not merely a stylistic preference.
- **Pessimistic write locking on the inventory row itself**, inside
  flight-service/hotel-service, not booking-service. `SELECT ... FOR UPDATE`
  only works within the DB connection/transaction that holds the lock — it
  cannot be held across an HTTP call to another service. So the
  reserve/release logic has to live in the service that owns the row, and
  booking-service calls it as an atomic internal operation.

## Alternatives considered
- **Optimistic locking (version column, retry on conflict)** on inventory
  rows. Better throughput under low contention, but this is exactly the
  opposite case: short transaction, high contention on a single row (a
  popular flight's last few seats), where correctness-by-blocking beats
  throughput-by-retrying.
- **Two-phase commit / distributed transactions** across all four services.
  Technically possible with some brokers, but requires every participant to
  support it, blocks resources for the duration of the whole saga, and is
  rarely used in real microservice systems for exactly that reason.

## Consequences
- Every internal reserve/release, authorize/refund, earn/reverse endpoint
  must be **idempotent** — a retried compensating call (e.g. after a
  timeout) must not double-release seats or double-refund. loyalty-service
  enforces this by checking for an existing transaction record per
  `bookingId` before acting.
- booking-service's cancel flow reuses the exact same compensating calls as
  Saga failure handling — cancellation is "run the same rollback, on
  purpose, later."
- This orchestrator becomes a single point of coordination logic. If a
  fifth participant were added, its compensating call would need to be
  wired into booking-service's failure handling by hand — a fully
  choreographed, event-driven design (Phase 5+ territory) would instead let
  the new participant subscribe to relevant events without booking-service
  knowing it exists. That flexibility is deliberately not what we're
  building yet.
