# ADR 0008: Distributed tracing and Prometheus metrics, scoped to what Docker's absence allows

**Status:** Accepted — 2026-07-10

## Context
Phase 6 calls for tracing, metrics, and centralized logging. Docker Desktop
was unavailable on the development machine throughout this phase (same
blocker as Phase 5's Kafka broker) — no Zipkin, Prometheus, or Grafana
server to actually run against.

## Decision
Split the work into what's verifiable without a collector and what isn't:
- **Tracing**: added `micrometer-tracing-bridge-brave` to every service.
  Configured `management.tracing.sampling.probability: 1.0` and a shared
  log pattern (`%X{traceId}`, `%X{spanId}`) via `config-server`'s shared
  config — centralized there since 9 of 11 services already import it,
  rather than duplicating the same two lines 9 times. Deliberately did
  **not** configure a Zipkin export endpoint — trace/span IDs are generated
  and propagated regardless of whether anything collects them, which is
  exactly the part verifiable without Docker.
- **Metrics**: added `micrometer-registry-prometheus` to every service and
  `prometheus` to each service's `management.endpoints.web.exposure.include`.
  Verified via direct `curl .../actuator/prometheus` that each service
  emits valid Prometheus-format text — proving our side of the integration
  without an actual Prometheus server scraping it.
- **Centralized logging**: consistent log format (service name + trace ID +
  span ID) across every service is now in place, which is the prerequisite
  for centralized log aggregation — but actually shipping logs to
  Loki/ELK is deferred to Phase 7 alongside Docker Compose, not attempted
  here.

## A real bug found and fixed along the way
The first end-to-end trace-propagation test showed **different** trace IDs
in `booking-service` and `flight-service` for the same logical request —
each service was silently starting its own new trace instead of continuing
the caller's. Root cause: `spring-cloud-starter-openfeign` does not pull in
`io.github.openfeign:feign-micrometer` transitively, and without it Feign
has no hook into Micrometer's `ObservationRegistry` to inject propagation
headers (`traceparent`) on outgoing requests. Fixed by adding
`feign-micrometer` explicitly to `booking-service` and `auth-service` (the
two Feign-client users). Re-verified: the same trace ID now appears across
`booking-service` → `flight-service` → `payment-service` → `loyalty-service`
for a single booking request, with distinct span IDs per hop — correct
distributed-tracing behavior.

A second, smaller issue: the new `/actuator/prometheus` and
`/actuator/circuitbreakers`/`/actuator/retries` endpoints returned 403 on
every service with Spring Security on its classpath, because only
`/actuator/health` was in each `SecurityConfig`'s permit-all list. Fixed by
adding the new endpoints there too — same dev-only-exposure caveat already
noted for `h2-console`.

## Consequences
- Nothing in this phase required Kafka or Docker, so it stayed fully
  live-verifiable despite the same environment constraint that limited
  Phase 5.
- The `feign-micrometer` gap is a good illustration of why "add tracing to
  the classpath" isn't automatically enough — Feign specifically needs an
  extra, easy-to-miss dependency that plain `RestTemplate`/`WebClient`
  usage doesn't.
- Actually running Zipkin/Prometheus/Grafana against this instrumentation,
  and shipping logs somewhere centralized, remains a Phase 7 follow-up
  alongside Docker Compose and the pending Kafka live test.
