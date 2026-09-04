## Why

Pantheon needs a working technical foundation before any business functionality can be built. Today the repository only contains the OpenSpec scaffold — there is no backend, messaging layer, or frontend. This change bootstraps the three initial services as a single monorepo (chosen to work well with AI-assisted, spec-driven development via OpenSpec) with the core technical capabilities each service needs, and prepares all three to run as simple standalone processes now while being structurally ready for containerized/Kubernetes deployment later.

## What Changes

- Scaffold `pantheon-service` (Java 17, Spring Boot): REST API, JPA persistence, OAuth2 login (Google and email/password), a RabbitMQ producer, and an SSE endpoint for pushing real-time events to clients.
- Scaffold `pantheon-message` (Java 17, Spring Boot): REST API, JPA persistence, API-token authentication for service-to-service calls (validates tokens issued to `pantheon-service`), and a RabbitMQ consumer.
- Scaffold `pantheon-web` (Vue 3, TypeScript, Vite, Tailwind CSS, Vue Router): SPA shell with an innovative, intuitive UI evoking technology and civil construction, full dark/light theming, and an SSE client that consumes the stream exposed by `pantheon-service`.
- Establish monorepo layout and shared conventions (build tooling, environment/config strategy, containerization) so all three services can each be run locally today and packaged as independent pods on Kubernetes later without restructuring.
- Define the RabbitMQ queue contract and message envelope shared between `pantheon-service` (producer) and `pantheon-message` (consumer).
- Define the API-token issuance/validation contract used for `pantheon-service` → `pantheon-message` authentication.

## Capabilities

### New Capabilities
- `pantheon-service`: Core Spring Boot service — REST API, JPA persistence, OAuth2 login (Google + email), RabbitMQ message publishing, and an SSE endpoint for real-time event streaming to the web client.
- `pantheon-message`: Spring Boot messaging worker — REST API, JPA persistence, API-token authentication for inbound service-to-service calls, and RabbitMQ message consumption.
- `pantheon-web`: Vue 3 SPA — app shell, dark/light theming, routing, and an SSE client that subscribes to `pantheon-service`'s event stream.
- `platform-foundation`: Monorepo structure, per-service local run/build setup, and containerization/Kubernetes-readiness (Dockerfiles, health/readiness probes, externalized config) shared across all three services.

### Modified Capabilities
- None — this is the initial bootstrap of the platform; no existing specs exist yet.

## Impact

- **New code**: three new top-level projects/directories (`pantheon-service`, `pantheon-message`, `pantheon-web`) plus shared monorepo tooling (root build/config files, CI wiring, Docker/K8s manifests).
- **New infrastructure dependencies**: RabbitMQ broker, a relational database (JPA-backed) for each Java service, Google OAuth2 client credentials.
- **New inter-service contracts**: RabbitMQ queue/message schema between `pantheon-service` and `pantheon-message`; API-token contract for authenticating `pantheon-service` → `pantheon-message` calls; SSE event contract between `pantheon-service` and `pantheon-web`.
- **No existing systems affected** — this is a greenfield bootstrap.
