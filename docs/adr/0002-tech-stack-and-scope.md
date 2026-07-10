# ADR 0002: Tech stack and full 9-phase roadmap (vs. leaner alternative)

**Status:** Accepted — 2026-07-10

## Context
Two candidate scopes were on the table when starting the build:
1. A leaner 6-phase, 10-service, H2-only, Docker-Compose-only spec.
2. A fuller 9-phase spec adding Kafka, Resilience4j, an observability stack
   (Micrometer/Zipkin, Prometheus/Grafana, Loki/ELK), Postgres+Redis,
   `loyalty-service`, Kubernetes+Helm, and GitHub Actions CI/CD.

## Decision
Build to the fuller 9-phase spec. The user's explicit goal for this project
is to go from "I know Spring Boot" to "I understand how a real production
microservices system is designed, built, deployed, and operated" ahead of
joining the real (enterprise-scale) Chase Travel project — the leaner spec
would skip most of the concepts that distinguish a toy CRUD system from a
production one.

## Consequences
- Longer build; more moving parts to keep green between phases.
- Each phase's services still start on H2 in-memory DBs and get migrated to
  Postgres/Redis in Phase 7, not before — avoids taking on infra complexity
  before the domain logic exists to test against it.
