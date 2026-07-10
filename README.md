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
5. Event-driven — Kafka, notification-service *(code complete, live end-to-end test pending Docker)*
6. Resilience & observability — Resilience4j, tracing, metrics, logs
7. Containerization + real DB — Docker Compose, H2 → Postgres, add Redis
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

**Current limitation, called out explicitly:** the "only ADMIN can write"
rules are enforced by each service trusting `X-User-Id`/`X-User-Roles`
headers set by `api-gateway` after JWT validation (see
[ADR 0004](docs/adr/0004-trust-the-gateway.md)). Running everything locally
via `mvn spring-boot:run`, every service's port is open on `localhost` —
nothing stops you from calling a service directly and forging those headers
to impersonate anyone. That trust boundary only becomes real in Phase 7,
once docker-compose stops publishing anything but the gateway's port to
the host.

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

*(Filled in once services exist — will include `docker compose up --build`,
demo credentials, gateway base URL, and a sample login → search → book flow.)*
