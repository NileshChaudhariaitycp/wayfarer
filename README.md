# Wayfarer Travel Platform

An original, non-proprietary learning replica of a Chase-Travel-style microservices
booking platform (flights + hotels). Built to learn how a real production
microservices system is designed, built, deployed, and operated — not a copy of
any proprietary code, branding, or UI.

See [`docs/adr/`](docs/adr/) for the reasoning behind major decisions,
[`GLOSSARY.md`](GLOSSARY.md) for plain-English term definitions as they come up,
and [`LEARNING-LOG.md`](LEARNING-LOG.md) for per-phase comprehension checkpoints.

## Status

🚧 Under active build. Phase 0–4 complete and fully verified end-to-end:
backbone (Eureka, Config Server, Gateway), identity (auth-service,
user-service, JWT + RBAC, gateway header forwarding), inventory
(flight-service, hotel-service, public search + admin CRUD), and
orchestration (booking-service Saga with pessimistic-locked inventory,
payment-service, loyalty-service) — including the compensation paths
(payment decline releases seats, cancellation reverses everything).

Phase 5 (Kafka + notification-service) is built and both services boot
cleanly, but **not yet live-verified end-to-end** — Docker Desktop failed to
start on the dev machine ("Docker Desktop is unable to start"), so there's
no Kafka broker to test the actual produce→consume flow against yet. What
*is* confirmed: bookings still complete successfully even with Kafka
unreachable (a real bug in the "fire and forget" publish path was found and
fixed along the way — see [ADR 0006](docs/adr/0006-event-driven-notifications.md)).
Run the live test as soon as Docker/Kafka is available.

Phase 6 (Resilience4j + tracing + Prometheus metrics) is complete and fully
live-verified — circuit breakers confirmed to open under repeated failure,
fail fast while open, and recover automatically; every service confirmed
emitting valid Prometheus-format metrics.

Phase 7 (Docker Compose, Postgres, Redis) is complete and fully
live-verified: all 11 services + 5 infrastructure containers (Postgres,
Redis, Kafka, Zipkin, Prometheus) build and run together; a real
register → login → book flow persists correctly to Postgres; a Kafka
`BookingEvent` is actually produced and consumed by `notification-service`
(closing Phase 5's deferred verification); a full 5-service distributed
trace (api-gateway → booking-service → hotel-service → payment-service →
loyalty-service) appears correctly in Zipkin for a single booking request
(closing Phase 6's deferred verification, after fixing a real
missing-dependency bug — see
[ADR 0009](docs/adr/0009-containerization-postgres-redis.md)); Prometheus
reports all 11 scrape targets healthy; and Redis caching on flight/hotel
search is verified correct on both the cache-miss and cache-hit path (a
second real bug — Jackson type information being lost on cache reads — was
found and fixed here too, see ADR 0009). Only `api-gateway`,
`discovery-server`, `zipkin`, and `prometheus` publish a port to the host
now — the "trust the gateway" boundary from Phase 2 is finally enforced by
the network, not just application code.

## Services

| # | Service | Port | Responsibility |
|---|---------|------|-----------------|
| 1 | `discovery-server` | 8761 | Eureka service registry |
| 2 | `config-server` | 8888 | Centralized configuration |
| 3 | `api-gateway` | 8080 | Single entry point: routing, JWT validation, identity-header forwarding |
| 4 | `auth-service` | 8081 | Register/login, JWT issuance, credentials & roles |
| 5 | `user-service` | 8082 | User profiles |
| 6 | `flight-service` | 8083 | Flight inventory + search |
| 7 | `hotel-service` | 8084 | Hotel/room inventory + search |
| 8 | `booking-service` | 8085 | Orchestrates bookings (Saga) across flight/hotel/payment/loyalty; pessimistic locking on inventory |
| 9 | `payment-service` | 8086 | Mock payment authorization/capture |
| 10 | `notification-service` | 8087 | Kafka consumer; mocks booking/payment confirmations |
| 11 | `loyalty-service` | 8088 | Loyalty/rewards points ledger |

## Roadmap

0. Orientation *(complete)*
1. Foundation — discovery/config/gateway *(complete)*
2. Identity — auth-service, user-service, full JWT + RBAC, gateway header forwarding *(complete)*
3. Inventory — flight-service, hotel-service, public search + admin CRUD *(complete)*
4. Orchestration — booking-service (Saga), payment-service, loyalty-service *(complete)*
5. Event-driven — Kafka, notification-service *(complete — live-verified in Phase 7)*
6. Resilience & observability — Resilience4j, tracing, metrics, logs *(complete — Zipkin/Prometheus live-verified in Phase 7)*
7. Containerization + real DB — Docker Compose, H2 → Postgres, add Redis *(complete)*
8. Deployment — Kubernetes + Helm on minikube/kind
9. CI/CD — GitHub Actions
10. Capstone — independent feature ticket, reviewed like a real PR

## Role matrix (implemented so far)

| Endpoint | Public | CUSTOMER | TRAVEL_AGENT | ADMIN |
|---|---|---|---|---|
| `POST /auth/register` | ✅ | ✅ | ✅ | ✅ |
| `POST /auth/login` | ✅ | ✅ | ✅ | ✅ |
| `GET /users/me` | ❌ | ✅ (own profile) | ✅ (own profile) | ✅ (own profile) |
| `GET /users/{id}` | ❌ | ❌ | ❌ | ✅ (any profile) |
| `GET /flights/search`, `GET /flights/{id}` | ✅ | ✅ | ✅ | ✅ |
| `POST/PUT/DELETE /flights/**` | ❌ | ❌ | ❌ | ✅ |
| `GET /hotels/search`, `GET /hotels/{id}` | ✅ | ✅ | ✅ | ✅ |
| `POST/PUT/DELETE /hotels/**` | ❌ | ❌ | ❌ | ✅ |
| `POST /bookings/flights`, `POST /bookings/hotels` | ❌ | ✅ (self only) | ✅ (self or on behalf, via `customerId`) | ✅ |
| `POST /bookings/{id}/cancel` | ❌ | ✅ (own booking) | ✅ (booking they created) | ✅ (any) |
| `GET /bookings/{id}`, `GET /bookings/me` | ❌ | ✅ (own only) | ✅ (own or created-by-them) | ✅ (any) |
| `GET /bookings` (list all) | ❌ | ❌ | ❌ | ✅ |
| `GET /loyalty/me` | ❌ | ✅ (own balance) | ✅ (own balance) | ✅ (own balance) |

The "only ADMIN can write" rules are enforced by each service trusting
`X-User-Id`/`X-User-Roles` headers set by `api-gateway` after JWT validation
(see [ADR 0004](docs/adr/0004-trust-the-gateway.md)). Running everything
locally via `mvn spring-boot:run`, every service's port is open on
`localhost` — nothing stops you from calling a service directly and forging
those headers to impersonate anyone. **Running via `docker compose up`
(Phase 7), that trust boundary is real**: only `api-gateway` publishes a
port to the host, so backend services are unreachable except through the
gateway's JWT validation.

## Demo credentials (seeded by each service's DataSeeder)

| Username | Password | Role |
|---|---|---|
| `customer1` | `Password123!` | CUSTOMER |
| `agent1` | `Password123!` | TRAVEL_AGENT |
| `admin1` | `Password123!` | ADMIN |

## Tech stack

Java 17 · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 · Spring Data JPA ·
Spring Security 6 (JWT, RBAC) · H2 → PostgreSQL + Redis · Kafka · OpenFeign ·
Resilience4j · Micrometer/Zipkin · Prometheus/Grafana · Docker · Kubernetes + Helm ·
GitHub Actions.

## Run it

```
docker compose up --build -d
```

Builds and starts all 11 services plus Postgres, Redis, Kafka, Zipkin, and
Prometheus on a shared `wayfarer-net` bridge network. First boot takes a few
minutes (11 separate Maven builds); subsequent starts are fast. Only
`api-gateway` (8080), `discovery-server` (8761), `zipkin` (9411), and
`prometheus` (9090) are reachable from the host — every backend service is
internal-only.

Sample flow through the gateway (`http://localhost:8080`):

```
# Register
curl -X POST localhost:8080/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"Password123!","email":"demo@example.com","fullName":"Demo User"}'
# -> {"token": "...", ...} — save the token

# Search flights (cached in Redis after the first call)
curl "localhost:8080/flights/search?origin=JFK&destination=LAX"

# Book (runs the full Saga: reserve seats -> authorize payment -> earn loyalty points -> publish Kafka event)
curl -X POST localhost:8080/bookings/flights -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"flightId":1,"seats":1,"cardToken":"tok_visa_test"}'
```

Or use the seeded [demo credentials](#demo-credentials-seeded-by-each-services-dataseeder)
instead of registering. Check `docker compose logs notification-service` for
the mock email confirmation, and `http://localhost:9411` (Zipkin) for the
resulting distributed trace.
