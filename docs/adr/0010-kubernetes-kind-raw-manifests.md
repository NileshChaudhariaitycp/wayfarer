# ADR 0010: Kubernetes deployment via kind, raw manifests before Helm

**Status:** Accepted — 2026-07-13

## Context
Phase 7 containerized every service and ran the full stack via Docker
Compose. Compose has no answer for a node dying, running multiple replicas
of a hot service, or rolling out a new version without downtime — it's a
single-machine orchestrator. Kubernetes' reconciliation-loop model
(declare desired state, a controller continuously corrects drift) is the
next step. This phase deploys the same 11 service images to a local
Kubernetes cluster, hand-written as raw YAML manifests first (by explicit
choice, to see exactly what a Deployment/Service/ConfigMap/Secret looks
like before any templating layer sits on top), with a Helm chart as the
planned follow-up refactor once the raw shape is understood.

## Decisions

**`kind` over minikube**, chosen by the user: kind runs cluster nodes as
plain Docker containers rather than a full VM, which has less additional
moving-parts overhead on a machine where Docker Desktop was already the
established, if occasionally flaky, foundation from Phases 5-7.

**Single-node cluster, not the originally-planned 1 control-plane + 2
workers.** The 3-node topology crashed Docker Desktop outright once real
workloads started — 3 full kubelet+containerd+control-plane containers,
plus Postgres/Redis/Kafka/Zipkin/Prometheus, plus 11 JVMs all starting
simultaneously, exceeded what this machine's Docker Desktop (4 CPUs /
7.75GB allocated) could sustain. Switched to single-node after the crash.
This is a deliberate right-sizing decision, not a lesser substitute: every
Deployment/Service/ConfigMap/Secret object, and the eventual Helm chart, is
identical either way — only node-distribution/scheduling become
unobservable, which was never this phase's core teaching goal.

**Every K8s `Service` is named identically to its docker-compose service
name** (`postgres`, `redis`, `kafka`, `flight-service`, etc.), all in the
same `default` namespace. This is a deliberate continuity choice: K8s
Service DNS resolves short names the same way Docker Compose's embedded DNS
did, so `docker/prometheus.yml`'s scrape target list needed zero changes to
work unmodified in `k8s/base/prometheus.yaml`.

**`imagePullPolicy: Never` on every custom service image**, loaded into the
cluster via `kind load docker-image` rather than pushed to any registry.
These images only exist locally; the default pull policy would make the
kubelet try to pull from Docker Hub and fail with `ErrImagePull`. A real
deployment would push to a private registry and use `IfNotPresent` or
`Always` instead — noted as a gap, not silently worked around.

**A shared `ConfigMap` (`wayfarer-common-config`) for env vars every
service needs** (`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`, `CONFIG_SERVER_URL`,
`MANAGEMENT_ZIPKIN_TRACING_ENDPOINT`, etc.), consumed via `envFrom`, with
service-specific vars (datasource URL, cache/Kafka settings) layered on top
via each Deployment's own `env:` list — the K8s equivalent of the repeated
`environment:` block every service had in `docker-compose.yml`.

**`config-server` explicitly overrides `SPRING_PROFILES_ACTIVE` back to
`native`** in its own manifest (an `env:` entry always wins over the same
key sourced from `envFrom:`). Its own `application.yml` hardcodes
`spring.profiles.active: native` to activate the classpath config-repo
backend; the shared ConfigMap's `SPRING_PROFILES_ACTIVE=docker` — needed by
every *other* service — would otherwise silently override it, and Spring
Cloud Config Server falls back to its default git backend, which fails
with "you need to configure a uri for the git repository." `docker-compose.yml`
never hit this because it never set `SPRING_PROFILES_ACTIVE` for
config-server at all.

**`startupProbe` on every service**, not just a long `initialDelaySeconds`
on `livenessProbe`. `livenessProbe`/`readinessProbe` don't even begin
checking until `startupProbe` succeeds once — this decouples "how long can
this container take to boot" (highly variable under CPU contention; we
observed 70-160+ second JVM starts even in Phase 7) from "how fast should
we notice this container has genuinely hung after it was already healthy."
A fixed `initialDelaySeconds` conflates the two and, set too short, kills
and restarts a container that just needed more time — which is exactly
what happened live to `discovery-server`/`config-server` before this fix.

**Domain services deployed in small batches** (2-3 Deployments at a time),
each verified stable (`0` restarts) before applying the next, rather than
one `kubectl apply` across all 9 at once. `docker-compose.yml`'s
`depends_on: condition: service_healthy` chains effectively staggered
startup for free; a flat `kubectl apply -f` over many manifest files does
not, and applying them all simultaneously creates a real thundering-herd
CPU contention problem on a resource-constrained single node — worse,
re-applying a "fix" to already-struggling Deployments can spawn a *second*
competing ReplicaSet/Pod per service instead of replacing the failing one,
doubling the contention rather than resolving it (hit live — see
LEARNING-LOG.md Phase 8).

## Alternatives considered
- **minikube.** Slightly heavier startup, more built-in addons via a single
  flag. Not chosen — see Decisions above.
- **A StatefulSet for Postgres.** More correct for stable pod
  identity/storage across rescheduling, or a managed operator
  (Zalando's postgres-operator, CloudNativePG) in a real deployment. Kept
  as a single-replica Deployment + PVC here, matching ADR 0009's "one
  Postgres instance" simplification — noted as a gap, not treated as
  equivalent to a real production setup.
- **An actual Ingress controller** (nginx-ingress, etc.) instead of a bare
  `NodePort` + kind's `extraPortMappings` for `api-gateway`. A real
  multi-service ingress story is Ingress's whole purpose, but for one
  externally-reachable service, a NodePort is the simpler, sufficient
  choice for this phase.

## Consequences
- The Secret holding Postgres credentials (`k8s/base/secret-postgres.yaml`)
  is base64-encoded, not encrypted — the same real gap ADR 0009 already
  flagged for docker-compose's plaintext password, wearing a different hat.
  A real deployment needs an actual secrets-management layer.
- Losing 2 of 3 planned nodes means pod scheduling/anti-affinity across
  nodes isn't observable in this environment — acceptable for this phase's
  goals, but worth revisiting if a future phase specifically wants to
  demonstrate node-level failure/rescheduling behavior.
- The staggered-batch deployment approach doesn't scale as a real practice
  — a production system with many more services needs a real answer to
  this (staged rollouts, readiness gates, GitOps sync waves), not a human
  manually watching `kubectl get pods` between `apply` calls. Noted here as
  a known simplification specific to this being a single learning machine's
  constrained resources, not a recommended pattern.
