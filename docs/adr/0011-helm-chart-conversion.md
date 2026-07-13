# ADR 0011: Helm chart — templatizing the raw manifests

**Status:** Accepted — 2026-07-13

## Context
ADR 0010 covers the raw, hand-written `k8s/base/*.yaml` manifests, written
first by deliberate choice so the underlying Kubernetes objects
(Deployment, Service, ConfigMap, Secret) were fully understood before any
templating layer sat on top. With that understanding in place, this ADR
covers converting them into a single installable Helm chart
(`helm/wayfarer/`).

## Decisions

**One parameterized template (`templates/services.yaml`) replaces 9
nearly-identical Deployment+Service manifest pairs.** Every domain
service's raw manifest differed only in name, port, and a handful of
env-var flags (has a datasource? needs Redis? needs Kafka? is it the one
NodePort-exposed service?). `values.yaml`'s `services:` list captures
exactly that variation as data; the template renders each entry via
`range`. This is the same DRY instinct as the parent POM's
`dependencyManagement` block from Phase 0 — one place to add a service or
change a port, instead of hand-editing N files.

**`discovery-server` and `config-server` stay in the same template**,
using the `commonEnv: false` / `envOverride` fields to express their real
differences (discovery-server needs no shared env vars at all;
config-server needs the shared ConfigMap's `SPRING_PROFILES_ACTIVE=docker`
overridden back to `native`) rather than being split into a separate
"backbone" template. They're not meaningfully different *shapes* of
Kubernetes object from the other 9 — just different *values* — so they
belong in the same data-driven template.

**Prometheus's scrape target list is generated from the same
`.Values.services` list**, instead of being a second, separately
maintained copy (which the raw `k8s/base/prometheus.yaml` was — it had to
be kept in sync with `k8s/base`'s service manifests by hand). This closes
a real drift risk: forgetting to add a new service's scrape target when
adding its Deployment.

**`maxTier` staged-rollout mechanism.** A single `helm install`/`helm
upgrade` renders and applies every template in one shot — exactly like the
all-at-once `kubectl apply -f k8s/base/` that caused a real thundering-herd
CPU contention incident during Phase 8's raw-manifest verification (see
ADR 0010): 9 JVMs starting simultaneously on a resource-constrained
single-node `kind` cluster starved each other of CPU badly enough that
`startupProbe`s never succeeded, and even the "fix" of re-applying with a
longer timeout made it worse by creating a second competing ReplicaSet per
service. Each service in `values.yaml` has a `tier` (1 = backbone through
5 = orchestration + gateway, matching the exact batches verified stable by
hand); `templates/services.yaml` only renders services whose `tier <=
.Values.maxTier`. Installing/upgrading with `--set maxTier=1`, then `2`,
`3`, `4`, `5` in sequence — waiting for each tier's pods to reach `0`
restarts before advancing — reproduces the same staggered rollout that
worked manually, but as a repeatable, scriptable values override instead
of a human remembering which `kubectl apply` command to run in which
order. This is a workaround for a real resource constraint on this specific
machine, not how a normal Helm install behaves — on a machine with more
CPU headroom, a single `helm install` at the default `maxTier: 5` would
work directly.

## Alternatives considered
- **Helm hooks with weighted pre-install Jobs** to sequence startup
  automatically instead of manual staged `--set maxTier=N` calls. More
  "automatic," but meaningfully more complex to write and debug than a
  values override — not worth it for a single learning machine's resource
  ceiling. Noted as the more production-appropriate answer (alongside
  readiness gates and GitOps sync waves) if this pattern needed to scale
  beyond manual operation.
- **Splitting into multiple charts** (one per tier) instead of one chart
  with a tier field. Rejected — it would fragment `values.yaml`'s single
  source of truth for what should be one conceptual deployment, and
  wouldn't reduce the actual amount of YAML or logic needed.

## Consequences
- `maxTier` is a workaround, not a feature — it should not be read as "this
  is how Helm charts normally handle rollout ordering." A reader taking
  this chart as a template for a less resource-constrained environment
  should default to `maxTier: 5` (the chart's default) and never need the
  staged upgrades at all.
- The Helm release fully replaces the raw `k8s/base/*.yaml` resources
  (same object names, so `kubectl delete -f k8s/base/` was required first
  to avoid ownership conflicts) — `k8s/base/` is kept in the repo as the
  Phase 8 raw-manifest historical artifact and teaching reference, not as
  a currently-deployed alternative to the Helm chart.
