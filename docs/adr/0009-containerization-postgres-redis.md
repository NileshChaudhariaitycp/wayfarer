# ADR 0009: Containerization, Postgres/Redis via env vars, and the real trust boundary

**Status:** Accepted — 2026-07-10

## Context
Every prior phase ran all 11 services as plain JVM processes on localhost,
against H2 in-memory databases. That's fine for fast local development, but
doesn't reflect a real deployment: no container isolation, no real
persistent database, no cache, and — despite ADR 0004 describing a "trust
the gateway" security boundary since Phase 2 — every service's port has
been open on `localhost` the entire time, meaning that boundary was never
actually enforced by anything.

## Decisions

**Layered-jar multi-stage Dockerfiles.** Each service's Dockerfile builds
from source inside a `maven:3.9-eclipse-temurin-17` stage (reproducible —
doesn't depend on the host's local Maven setup), extracts Spring Boot's
layered jar, then assembles a minimal `eclipse-temurin:17-jre-jammy`
runtime image from just those layers. Maven's reactor needs every module's
`pom.xml` present to resolve `-pl <service>` even when building one module
in isolation, so every service's POM (not source) is copied into each
Dockerfile's build context — this keeps the Docker layer cache useful,
since a source change in one service doesn't invalidate every other
service's build.

**Postgres/Redis/Kafka/Zipkin endpoints via environment variables, not
Spring profiles.** Rather than adding a `docker` profile block to 7+
`application.yml` files, `docker-compose.yml` overrides the exact same
property keys (`SPRING_DATASOURCE_URL`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`,
etc.) via Spring Boot's relaxed environment-variable binding. This is the
standard 12-factor approach: never bake environment-specific hostnames into
the jar itself. H2 stays the default for local `mvn spring-boot:run`;
nothing about the jar changes between environments, only what's injected
around it.

**One Postgres container, seven databases** (`docker/postgres-init.sql`),
matching the H2 database-per-service names each service already used
locally — same boundary, real DBMS instead of in-memory. `ddl-auto: update`
is kept as-is for this environment too — noted explicitly as a real
production gap (see Consequences).

**Redis caching added where it has an actual justification**: flight/hotel
search results (`@Cacheable`, 60s TTL under Redis; local dev uses Spring's
in-memory "simple" cache type, no Redis dependency needed for fast local
iteration). Deliberately *not* evicted by `reserveSeats`/`releaseSeats` — a
stale cached seat count can only ever show a seat as available that's
actually gone by booking time, which `reserveSeats`'s pessimistic lock
(ADR 0005) already re-validates and correctly rejects. The cache is never
in the actual reservation path, so it cannot cause overbooking.

**Kafka in KRaft mode** (no separate Zookeeper container) — the modern,
simpler single-broker setup, using the official `apache/kafka` image.

**Host port policy — the real trust boundary.** Only `api-gateway` (business
traffic), `discovery-server` (Eureka dashboard), `zipkin`, and `prometheus`
(both observability UIs) publish a port to the host. Every backend service,
and postgres/redis/kafka, are reachable only from other containers on
`wayfarer-net`. This is the first point in the project where ADR 0004's
"trust the gateway" pattern is backed by actual network enforcement, not
just application code that assumed it.

## Alternatives considered
- **One Postgres container per service.** Closer to true microservice
  database isolation, but heavier for a learning project's resource
  footprint with no meaningful teaching payoff beyond what one instance
  with separate databases already demonstrates.
- **Docker BuildKit cache mounts for `~/.m2`** (`--mount=type=cache`) to
  speed up rebuilds. Real optimization, deliberately deferred — noted here
  rather than silently skipped: worth adding if build times become a
  friction point, but not essential to a correct first containerized
  deployment.

## Consequences
- `ddl-auto: update` against a real Postgres instance is not something a
  real production system should do — schema changes should go through
  versioned migrations (Flyway/Liquibase), so a bad migration can be rolled
  back and reviewed like any other code change. Kept as-is here to avoid
  scope creep beyond this phase's actual goal (containerization + real
  datastores), flagged explicitly as a gap rather than silently accepted.
- `docker-compose.yml` now has real secrets (the Postgres password) in
  plaintext — acceptable for a local learning deployment, explicitly not
  how a real production system would handle credentials (would use a
  secrets manager or at minimum `.env` file excluded from version control).
- The trust boundary finally being real means the direct-port-bypass
  exercise from Phase 2's LEARNING-LOG entry ("call user-service directly
  on 8082") **no longer works** once running via docker-compose — a good
  moment to revisit that question with the new answer.

## Bugs found during live verification (not caught by compile/unit checks)
Both of these only surfaced once the full stack actually ran against real
Postgres/Redis/Zipkin containers — see LEARNING-LOG.md Phase 7 for the
comprehension questions.

- **Redis cache serialization was silently type-lossy.** `CacheConfig`
  originally built `GenericJackson2JsonRedisSerializer` from Spring's
  autoconfigured `ObjectMapper` directly. Writes succeeded (no type
  information needed to serialize), but reads had no way to tell
  `List<FlightResponse>` apart from `List<LinkedHashMap>`, corrupting the
  cached value on every cache hit. Adding Jackson default typing
  (`activateDefaultTyping`) "fixed" the crash but produced inconsistent
  wrapper formats for non-"natural" scalar types (`BigDecimal` in
  particular). The final fix binds a `Jackson2JsonRedisSerializer` to the
  exact `JavaType` each cache holds (`flightSearch` → `List<FlightResponse>`,
  `hotelSearch` → `List<HotelResponse>`) via `RedisCacheManagerBuilderCustomizer`,
  avoiding default typing — and its associated deserialization-gadget
  security exposure — entirely.
- **Zipkin never received a single span**, despite correct sampling config
  and correct trace/span IDs appearing in every service's log output. Every
  service's `pom.xml` had `micrometer-tracing-bridge-brave` (creates and
  correlates trace context) but was missing `io.zipkin.reporter2:zipkin-reporter-brave`
  (actually exports completed spans to a Zipkin server). Added to all 10
  client services' POMs. This was a silent gap since Phase 6 — nothing in
  that phase's local (non-Docker) verification could have caught it, since
  no Zipkin server was ever actually reachable until this phase.
