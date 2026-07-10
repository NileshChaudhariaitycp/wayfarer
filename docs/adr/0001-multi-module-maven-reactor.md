# ADR 0001: Multi-module Maven reactor with a shared parent POM

**Status:** Accepted — 2026-07-10

## Context
We're building 11 Spring Boot services that must stay on consistent, compatible
dependency versions (Boot 3.5.16 + Spring Cloud 2025.0.3), and need a single
command to build/verify all of them together during early development.

## Decision
One root `pom.xml` (`packaging=pom`) lists every service as a `<module>`,
extends `spring-boot-starter-parent:3.5.16` for plugin management, and imports
the `spring-cloud-dependencies:2025.0.3` BOM in `dependencyManagement`. Each
service's own `pom.xml` extends this parent and only declares the starters it
actually needs — versions are inherited, never repeated.

## Alternatives considered
- **Independent repos per service**, each with its own POM and CI pipeline.
  This is what many mature microservice orgs run in production, since it lets
  teams release services independently. Rejected for now: at this stage we'd
  be fighting version drift across 11 repos instead of learning the domain.
  Worth revisiting if/when this project adds per-service CI (Phase 9).

## Consequences
- A single `mvn compile` / `mvn package` at the repo root builds everything.
- Every service is guaranteed to be on the same Boot/Cloud version pair.
- All services currently share one git history — a deliberate trade-off for
  a learning project, not a recommendation for how the real Chase Travel repo
  is necessarily organized.
