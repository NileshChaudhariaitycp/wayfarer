# Learning Log

Per-phase comprehension checkpoints. Each phase adds a section once it's complete.

## Phase 0 — Repo scaffold
- What is a Maven multi-module reactor, and why use one instead of 11 independent repos for this project?
- Why does Spring Cloud have its own "release train" version instead of matching Spring Boot's version number exactly?
- What would happen if `flight-service` and `api-gateway` ended up on different Spring Boot patch versions?

## Phase 1 — Backbone (discovery-server, config-server, api-gateway)
- If `config-server` crashed right now, would `api-gateway` immediately stop routing to it — or only fail on the *next* new request? What does that tell you about how `lb://` resolution actually works?
- Why does `spring.cloud.gateway.server.webflux.routes` use `lb://CONFIG-SERVER` instead of a hardcoded `http://localhost:8888`? What would break if we'd hardcoded it?

## Phase 2 — Identity (auth-service, user-service, gateway JWT validation)
- Walk through what happens, service by service, from `POST /auth/login` to a subsequent authorized `GET /users/me` — where is the JWT parsed, and where is it never seen again?
- Why does `user-service` return 403 (not 401) when a CUSTOMER requests someone else's profile via `/users/{id}`? What's the difference between the two status codes, semantically?
- The registration flow does a synchronous Feign call from auth-service to user-service *inside* the same request. What happens to the system's state if user-service is down at that exact moment? Is that acceptable, and why did we accept it for now instead of fixing it immediately?
- Right now, is anything actually stopping you from calling `user-service` on port 8082 directly, skipping the gateway and its JWT check entirely? Try it. What does that tell you about how much protection "trust the gateway" is really providing today, locally?

## Phase 3 — Inventory (flight-service, hotel-service)
- Why does the gateway need a *method-aware* public-path check for `/flights/**` and `/hotels/**`, when `/auth/register` and `/auth/login` were fine with a plain path match?
- `RoomType.hotel` is `@ManyToOne(fetch = FetchType.LAZY)` but `Hotel.roomTypes` is `fetch = FetchType.EAGER`. What's the practical difference, and why might always-eager loading of a collection become a problem as hotel-service's data grows?
- `flight-service`'s `update()` recomputes `seatsAvailable` from `totalSeats - seatsBooked` instead of just overwriting it with the request's value. Why does that matter once real bookings exist?

## Phase 4 — Orchestration (booking-service Saga, payment-service, loyalty-service, pessimistic locking)
- `BookingOrchestrationService` is deliberately NOT wrapped in one `@Transactional` spanning the whole Saga. Walk through what would go wrong if it were, specifically around the moment right after seats are reserved but before payment is attempted.
- During testing, a real "bug" turned out to be a test-data collision: an earlier manual call to loyalty-service's `/earn` endpoint used `bookingId=1`, and booking-service's first real booking also got `id=1`, so the idempotency check silently skipped crediting points. What does this tell you about `bookingId` as a dedup key across two independently-seeded systems, and would this scenario ever actually happen in production?
- `flight-service`'s `releaseSeats` is explicitly *not* idempotent (documented in a comment), while `loyalty-service`'s `earn`/`reverse` are. Why was that an acceptable trade-off here, and what would have to be true for it to stop being acceptable?
- Trace what happens if `payment-service` itself crashes (not declines — actually goes unreachable) mid-authorize, after `booking-service` already reserved seats. What state is the system left in, and who or what would need to notice and fix it?
