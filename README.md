# Wayfarer Travel Platform

An original, non-proprietary learning replica of a Chase-Travel-style microservices
booking platform (flights + hotels). Built to learn how a real production
microservices system is designed, built, deployed, and operated — not a copy of
any proprietary code, branding, or UI.

See [`docs/adr/`](docs/adr/) for the reasoning behind major decisions,
[`GLOSSARY.md`](GLOSSARY.md) for plain-English term definitions as they come up,
and [`LEARNING-LOG.md`](LEARNING-LOG.md) for per-phase comprehension checkpoints.

## Status

🚧 Under active build — see the roadmap below for current phase.

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
1. Foundation — discovery/config/gateway + one trivial service
2. Core domain & security — user/auth services, full JWT + RBAC
3. Orchestration — OpenFeign + Saga across booking/payment/loyalty
4. Event-driven — Kafka, notification-service
5. Resilience & observability — Resilience4j, tracing, metrics, logs
6. Containerization + real DB — Docker Compose, H2 → Postgres, add Redis
7. Deployment — Kubernetes + Helm on minikube/kind
8. CI/CD — GitHub Actions
9. Capstone — independent feature ticket, reviewed like a real PR

## Tech stack

Java 17 · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 · Spring Data JPA ·
Spring Security 6 (JWT, RBAC) · H2 → PostgreSQL + Redis · Kafka · OpenFeign ·
Resilience4j · Micrometer/Zipkin · Prometheus/Grafana · Docker · Kubernetes + Helm ·
GitHub Actions.

## Run it

*(Filled in once services exist — will include `docker compose up --build`,
demo credentials, gateway base URL, and a sample login → search → book flow.)*
