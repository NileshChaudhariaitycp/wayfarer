# ADR 0004: Gateway validates JWTs once; downstream services trust headers

**Status:** Accepted — 2026-07-10

## Context
Every protected request needs to be authenticated and authorized. That
JWT-parsing logic could live in every service, or in exactly one place.

## Decision
`api-gateway` is the only place that parses and verifies a JWT's signature.
On success, it turns the token's claims into `X-User-Id` / `X-User-Roles`
headers on the outgoing request. Every backend service (starting with
`user-service`'s `GatewayHeaderAuthenticationFilter`) trusts those headers
directly — it never sees or parses a raw JWT.

## Alternatives considered
- **Re-validate the JWT in every service.** More resilient (a service is
  correctly protected even if reached directly, bypassing the gateway), but
  duplicates the signing-secret distribution and validation logic across
  every service, and is more than this phase needs.

## Consequences
- This model's security guarantee holds only if backend services are
  genuinely unreachable except through the gateway. Anyone who can reach
  `user-service` directly can forge `X-User-Id`/`X-User-Roles` and
  impersonate any user or role — there is nothing in `user-service` itself
  stopping that right now.
- Phase 7's docker-compose only publishes the gateway's port (8080) to the
  host for exactly this reason — network isolation is what actually enforces
  the trust boundary this ADR assumes. Until then (running everything
  locally via `mvn spring-boot:run`), that boundary does not actually exist
  — every service's port is open on localhost. This is fine for local
  development but worth being explicit about.
- Defense-in-depth (re-validating the JWT downstream in addition to trusting
  headers) is a reasonable production hardening step, deliberately not done
  here to keep the pattern being taught clear.
