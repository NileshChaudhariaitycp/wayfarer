# ADR 0007: Circuit breaker everywhere, automatic retry only where it's safe

**Status:** Accepted — 2026-07-10

## Context
booking-service's Saga makes multiple sequential calls to other services
(flight/hotel-service, payment-service, loyalty-service). Any of those
calls can be slow or fail, and the naive instinct is "wrap every remote
call with retry + circuit breaker." But automatic retry is only safe on
**idempotent** operations — calling it twice must have the same effect as
calling it once.

Auditing what Phase 4 actually built: `getFlight`/`getHotel` are read-only
(naturally idempotent). `earn`/`reverse` (loyalty-service) and `refund`
(payment-service) are explicitly guarded to be idempotent — see their
Phase 4 implementations. But `reserveSeats`/`reserveRooms`/`releaseSeats`/
`releaseRooms` are documented as **not** idempotent (calling `reserveSeats`
twice reserves twice), and `authorize` creates a brand-new `Payment` row on
every call, so retrying it risks double-charging.

## Decision
Every downstream call goes through a `Resilient*Client` wrapper
(`ResilientFlightClient`, `ResilientHotelClient`, `ResilientPaymentClient`,
`ResilientLoyaltyClient`) instead of the raw Feign client:
- **Circuit breaker** wraps every method, on every client — if a downstream
  service is failing repeatedly, stop hammering it and fail fast instead of
  piling up slow/timing-out calls.
- **Automatic retry** (`@Retry`) is added *only* to the methods that are
  actually idempotent: `getFlight`, `getHotel`, `refund`, loyalty's `earn`
  and `reverse`. `reserveSeats`, `releaseSeats`, `reserveRooms`,
  `releaseRooms`, and `authorize` get circuit breaker protection but never
  automatic retry.
- Feign's own `connectTimeout`/`readTimeout` bound how long a single call
  can take, rather than Resilience4j's `TimeLimiter` — `TimeLimiter`
  requires the guarded method to return a `CompletableFuture` and run
  async, which would mean restructuring the whole synchronous Saga flow.
  Feign-level timeouts achieve the same practical bound without that
  architectural change.
- Every failure mode (Feign exception, circuit breaker OPEN, retries
  exhausted) collapses to one `DownstreamCallException` via each wrapper's
  fallback method, so `BookingOrchestrationService` has one exception type
  to catch regardless of which library or mechanism actually failed.

## Alternatives considered
- **Retry everywhere, "it'll probably be fine."** Rejected outright —
  retrying `reserveSeats` after a timeout where the first attempt actually
  succeeded server-side (response just got lost) would double-reserve a
  seat. This is the single most important thing to get right in this ADR.
- **Resilience4j `TimeLimiter` for the timeout leg.** More idiomatic
  Resilience4j, but forces every guarded method to become
  `CompletableFuture`-returning and run on a separate executor — a bigger
  change than the value justified here. Feign-level timeouts are a
  pragmatic, simpler substitute; noted as a real trade-off, not a
  correctness gap.

## Consequences
- If a compensating call itself fails after the circuit breaker gives up
  (e.g. `releaseSeats` during payment-failure handling), that exception
  propagates uncaught as a 500 — not retried or queued. A production system
  would route a failed compensation to a dead-letter queue or manual-
  intervention alert. Explicitly out of scope here; documented in
  `BookingOrchestrationService`'s class javadoc.
- Circuit breaker state and retry metrics are exposed via
  `/actuator/health` (component: `circuitBreakers`) and
  `/actuator/circuitbreakers` / `/actuator/retries` — useful for the
  observability work later in this phase.
