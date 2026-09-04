## Context

The repository is currently empty except for the OpenSpec scaffold. This change bootstraps a monorepo with three independent services that will eventually support a construction/technology-themed product. Today the only requirement is the technical foundation — auth, messaging, real-time push, and a themed UI shell — with no business domain entities yet. A hard constraint from the proposal: the three services must run as simple standalone processes today, but must not need restructuring to run as Kubernetes pods later.

## Goals / Non-Goals

**Goals:**
- Stand up runnable skeletons for `pantheon-service`, `pantheon-message`, and `pantheon-web` with the requested tech stacks.
- Define the three inter-service contracts up front: RabbitMQ message envelope (`pantheon-service` → `pantheon-message`), API-token auth (`pantheon-service` → `pantheon-message`), and SSE event contract (`pantheon-service` → `pantheon-web`).
- Keep each service 12-factor (externalized config, stateless, health-checked) so containerizing and running under Kubernetes later is a packaging exercise, not a rewrite.
- Keep local development simple: each service runnable on its own, plus a docker-compose file for shared infra (RabbitMQ, databases).

**Non-Goals:**
- No business/domain entities (e.g., actual construction-project data models) — that's future work on top of this foundation.
- No production secrets management, token rotation, or IdP hardening — placeholders and env-var based config only.
- No actual Kubernetes cluster, Helm charts, or CI/CD pipeline — only Dockerfiles and baseline manifests that prove the services are pod-ready.
- No horizontal-scaling support for SSE across multiple replicas — single-instance SSE only for now (see Risks).

## Decisions

### Monorepo layout
```
/pantheon-service   (Java 17 / Spring Boot)
/pantheon-message   (Java 17 / Spring Boot)
/pantheon-web        (Vue 3 / TypeScript)
/infra               (docker-compose.yml, Dockerfiles, baseline k8s manifests)
/openspec
```
Each service keeps its own build file and lifecycle (independent deploy units), not a single reactor build — this matches "simple microservices" and avoids coupling releases. A shared **parent/BOM POM** (`infra/build/pantheon-parent`) pins common dependency versions (Spring Boot BOM, JPA, RabbitMQ client, JWT lib) so both Java services stay aligned without sharing a deploy lifecycle.
*Alternative considered*: Gradle/Maven multi-module reactor — rejected because it couples build/versioning of two services that should deploy independently.

### Authentication in `pantheon-service`
Spring Security handles two login paths — OAuth2 (Google, via `spring-boot-starter-oauth2-client`) and email/password (`DaoAuthenticationProvider` against the local `users` JPA table). Both paths converge on a single **self-issued JWT** returned to the client; all subsequent REST/SSE calls authenticate with that JWT, regardless of how the user logged in.
*Alternative considered*: server-side session cookies — rejected because it complicates the SPA + SSE + potential future multi-pod setup (sticky sessions), whereas a stateless JWT works uniformly.

### API-token auth in `pantheon-message`
`pantheon-service` authenticates to `pantheon-message` with a pre-shared API token sent as an `X-API-Token` header, validated by a `OncePerRequestFilter` in `pantheon-message` against a value from externalized config (env var / secret). This is intentionally the simplest viable mechanism for the bootstrap; it sits behind a small `ApiTokenValidator` interface so it can be swapped for a DB-backed, rotatable token scheme later without touching callers.
*Alternative considered*: mutual TLS — deferred as unnecessary complexity for the initial bootstrap.

### RabbitMQ contract
A durable topic exchange `pantheon.events` with routing keys per event type (e.g., `message.created`). `pantheon-service` publishes; `pantheon-message` binds a durable queue (`pantheon-message.inbox`) to the routing keys it cares about. Message body is a JSON envelope: `{ "type": string, "payload": object, "occurredAt": ISO-8601, "correlationId": uuid }`.
*Alternative considered*: direct queue coupling (service publishes straight to a named queue) — rejected in favor of a topic exchange so additional consumers can be added later without changing the producer.

### SSE contract (`pantheon-service` → `pantheon-web`)
`pantheon-service` exposes `GET /api/sse/subscribe` (authenticated via the JWT above) returning a `text/event-stream` backed by Spring's `SseEmitter`, independent of the RabbitMQ flow — SSE is for pushing application events to the browser, RabbitMQ is for service-to-service work distribution. `pantheon-web` wraps the browser `EventSource` API in a `useSse()` composable that handles reconnect/backoff and typed event parsing.

### Data persistence
Each Java service owns its own database (database-per-service), PostgreSQL, accessed via Spring Data JPA with Flyway migrations from the first commit so schema evolution is versioned from day one.

### Frontend theming & structure
Tailwind CSS with `darkMode: 'class'` and a small design-token palette evoking "blueprint/technology + civil construction" (steel/graphite neutrals, blueprint-blue primary, safety-orange accent), toggled and persisted (e.g., `localStorage` + `prefers-color-scheme` fallback). Vue Router provides the page shell; Vite is the build tool; TypeScript throughout.

### Container / Kubernetes readiness
Each service gets a multi-stage Dockerfile and exposes Spring Boot Actuator `/actuator/health/liveness` and `/actuator/health/readiness` (Java services) so they can be wired to k8s probes later. Config is fully externalized via environment variables (no hardcoded profiles for secrets). `infra/k8s/` gets baseline (not-yet-applied) Deployment/Service/ConfigMap manifests per service to prove the shape works, without standing up a real cluster now. Locally, `infra/docker-compose.yml` runs RabbitMQ + Postgres(x2) + the three services for simple end-to-end testing.

## Risks / Trade-offs

- **[Risk]** Hybrid OAuth2 + email login adds auth surface area early → **Mitigation**: converge both paths on one self-issued JWT so downstream code only ever deals with one token format.
- **[Risk]** Shared pre-shared API token is a weak, static secret → **Mitigation**: isolate behind `ApiTokenValidator`; documented as an interim mechanism to replace with rotatable, DB-backed tokens before production use.
- **[Risk]** Single-instance `SseEmitter` state doesn't survive multiple `pantheon-service` pod replicas (a client's stream is pinned to the pod that accepted it) → **Mitigation**: acceptable for the initial bootstrap (single replica); documented as an open question to revisit (e.g., RabbitMQ fanout or Redis pub/sub backplane) before scaling horizontally.
- **[Risk]** Building k8s-readiness now (health probes, manifests) before there's a cluster adds upfront effort → **Mitigation**: kept minimal — Actuator endpoints and plain YAML templates only, no Helm/CI investment yet.

## Migration Plan

Greenfield bootstrap — no production data or running system to migrate. Suggested build order to respect dependencies: (1) monorepo/infra foundation (docker-compose, parent POM, shared conventions), (2) `pantheon-message` (fewest dependencies, defines the token/queue contract it expects), (3) `pantheon-service` (implements the producer/token-client side, plus auth and SSE), (4) `pantheon-web` (consumes `pantheon-service`'s REST/SSE contract last). Rollback, if needed, is simply removing the relevant directories — no persisted state exists yet.

## Open Questions

- Confirm PostgreSQL as the JPA datastore for both Java services (assumed here).
- Long-term secret/token management approach (Vault, k8s Secrets, cloud KMS) — deferred past this bootstrap.
- SSE fan-out strategy once `pantheon-service` needs more than one replica.
- Whether `pantheon-web` stays pure client-side (Vite SPA) or gains SSR needs later — assumed CSR-only for now.
