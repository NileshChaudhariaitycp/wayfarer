# Glossary

Plain-English definitions of terms as they're introduced, in the order we hit them.

| Term | Plain-English definition |
|---|---|
| Maven multi-module reactor | One parent `pom.xml` that lists several sub-projects ("modules") and builds them together in dependency order, sharing version management. |
| BOM (Bill of Materials) | A `pom.xml` with `packaging=pom` that pins compatible versions of a whole family of libraries, so you declare a dependency without specifying its version — the BOM decides it. |
| Spring Cloud "release train" | Spring Cloud isn't versioned 1:1 with Spring Boot. Instead, a named release (e.g. `2025.0.x` "Northfields") bundles compatible versions of Eureka, Gateway, Config, OpenFeign, etc. for a specific Spring Boot minor version. |
| Service registry (Eureka) | A directory that services register themselves into on startup ("I'm `flight-service`, I'm at `10.0.0.5:8083`") and query at runtime to find each other, instead of hardcoding IPs/ports. |
| JWT (JSON Web Token) | A signed, self-contained token: a client presents it instead of re-sending credentials on every request. Anyone holding the signing key/secret can verify it wasn't tampered with, without a database lookup. |
| BCrypt | A deliberately slow password-hashing algorithm — the slowness is the point, it makes brute-forcing a stolen hash expensive. Never store plaintext or fast-hashed (e.g. plain SHA-256) passwords. |
| "Trust the gateway" | A pattern where only the API gateway validates JWTs; it forwards trusted identity headers (`X-User-Id`, `X-User-Roles`) to backend services, which trust those headers instead of re-parsing tokens. Only safe if backend services are unreachable except through the gateway — see ADR 0004. |
| OpenFeign | A declarative REST client: you write a Java interface with annotations describing an HTTP call, and Spring generates the actual HTTP call at runtime — including resolving the target service's address via Eureka. |
| `@PreAuthorize` | A Spring Security annotation that guards a method with a permission check (e.g. `hasRole('ADMIN')`), evaluated before the method body runs. Requires `@EnableMethodSecurity`. |
| `@OneToMany` / `@ManyToOne` | JPA relationship mapping — one `Hotel` has many `RoomType`s, each `RoomType` belongs to one `Hotel`. `cascade = ALL` + `orphanRemoval = true` means saving/deleting the Hotel saves/deletes its RoomTypes too, since they have no independent lifecycle. |
