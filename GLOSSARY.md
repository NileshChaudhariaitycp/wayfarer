# Glossary

Plain-English definitions of terms as they're introduced, in the order we hit them.

| Term | Plain-English definition |
|---|---|
| Maven multi-module reactor | One parent `pom.xml` that lists several sub-projects ("modules") and builds them together in dependency order, sharing version management. |
| BOM (Bill of Materials) | A `pom.xml` with `packaging=pom` that pins compatible versions of a whole family of libraries, so you declare a dependency without specifying its version — the BOM decides it. |
| Spring Cloud "release train" | Spring Cloud isn't versioned 1:1 with Spring Boot. Instead, a named release (e.g. `2025.0.x` "Northfields") bundles compatible versions of Eureka, Gateway, Config, OpenFeign, etc. for a specific Spring Boot minor version. |
| Service registry (Eureka) | A directory that services register themselves into on startup ("I'm `flight-service`, I'm at `10.0.0.5:8083`") and query at runtime to find each other, instead of hardcoding IPs/ports. |
