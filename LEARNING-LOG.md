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
