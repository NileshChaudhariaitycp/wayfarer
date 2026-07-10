# ADR 0003: Separate auth-service (credentials) from user-service (profile)

**Status:** Accepted — 2026-07-10

## Context
Every user needs both a way to log in (username, password hash, role) and a
profile (email, full name, preferences). These could live in one service and
one table, or be split.

## Decision
`auth-service` owns credentials only: username, password hash, role. It knows
nothing about email or full name. `user-service` owns the profile — including
a copy of username and role for its own read purposes — keyed by the same id
`auth-service` assigned to the credential. `auth-service` calls `user-service`
(via OpenFeign) right after registration to create the matching profile.

## Alternatives considered
- **One combined `users` table/service.** Simpler for this phase, but
  conflates two concerns with very different security postures: credential
  handling needs tight, minimal-surface controls (rate limiting, audit
  logging, breach-response procedures) and changes rarely; profile data
  changes often, via many different feature code paths. Bundling them makes
  every profile-editing feature also "security-sensitive code."

## Consequences
- Registration is now a distributed operation: a credential save in
  auth-service's DB followed by a synchronous cross-service call to
  user-service. If that call fails, the credential exists with no profile —
  a real partial-failure state, not hidden or handled yet. This is a known,
  explicitly-accepted simplification for Phase 2; Phase 4's Saga pattern in
  booking-service is the proper fix for this class of problem across
  multiple services, with compensating actions instead of hoping nothing
  fails.
- The two services' seed data are coupled by an assumed id ordering
  (customer1=1, agent1=2, admin1=3) — fragile, and called out explicitly in
  both DataSeeders. Phase 5's event-driven approach (Kafka) removes this by
  having user-service react to a real "UserRegistered" event instead of two
  services independently guessing the same id.
