## 1. Monorepo & Local Infra Foundation

- [x] 1.1 Create top-level directories: `pantheon-service/`, `pantheon-message/`, `pantheon-web/`, `infra/`
- [x] 1.2 Create shared Maven parent/BOM (`infra/build/pantheon-parent`) pinning Spring Boot, JPA, RabbitMQ client, and JWT library versions for the two Java services
- [x] 1.3 Create `infra/docker-compose.yml` with RabbitMQ, one Postgres instance per Java service, and service entries for all three apps
- [x] 1.4 Document required local environment variables (DB URLs, RabbitMQ URL, API token, OAuth2 client id/secret, JWT signing key) in a root `.env.example`

## 2. pantheon-message: Project Setup

- [x] 2.1 Scaffold Spring Boot 4 (Java 17) project with `web`, `data-jpa`, and `amqp` starters
- [x] 2.2 Configure Postgres datasource and add Flyway with an initial migration for the processed-message table
- [x] 2.3 Wire Actuator with `/actuator/health/liveness` and `/actuator/health/readiness` endpoints

## 3. pantheon-message: API-Token Authentication & REST

- [x] 3.1 Implement `ApiTokenValidator` interface with an environment-variable-backed implementation
- [x] 3.2 Add a security filter that requires a valid `X-API-Token` header on protected endpoints, returning 401 on missing/invalid token
- [x] 3.3 Add a REST endpoint to list/retrieve processed message records for verification

## 4. pantheon-message: RabbitMQ Consumer & Persistence

- [x] 4.1 Declare the `pantheon.events` topic exchange, `pantheon-message.inbox` durable queue, and routing-key bindings
- [x] 4.2 Implement a listener that consumes the JSON message envelope (`type`, `payload`, `occurredAt`, `correlationId`)
- [x] 4.3 Persist each successfully processed message via JPA; do not acknowledge on processing failure (support retry/dead-letter)

## 5. pantheon-service: Project Setup

- [x] 5.1 Scaffold Spring Boot 4 (Java 17) project with `web`, `data-jpa`, `security`, `oauth2-client`, and `amqp` starters
- [x] 5.2 Configure Postgres datasource and add Flyway with an initial migration for the users table
- [x] 5.3 Wire Actuator with `/actuator/health/liveness` and `/actuator/health/readiness` endpoints

## 6. pantheon-service: Authentication

- [x] 6.1 Configure Google OAuth2 login via `spring-boot-starter-oauth2-client`, mapping successful login to a local user record (create-if-missing)
- [x] 6.2 Implement email/password registration and login using `DaoAuthenticationProvider` against the local users table
- [x] 6.3 Implement unified JWT issuance shared by both login paths, and a filter that authenticates REST/SSE requests via that JWT
- [x] 6.4 Return 401 for expired/invalid/missing tokens on protected endpoints

## 7. pantheon-service: RabbitMQ Producer

- [x] 7.1 Declare/reuse the `pantheon.events` topic exchange and publish the JSON message envelope on qualifying domain actions
- [x] 7.2 Configure an `ApiTokenClient` that attaches the shared `X-API-Token` header when calling `pantheon-message`'s REST API
- [x] 7.3 Add logging/error handling so publish failures to RabbitMQ are surfaced rather than silently swallowed

## 8. pantheon-service: Server-Sent Events

- [x] 8.1 Implement `GET /api/sse/subscribe` using `SseEmitter`, authenticated via the JWT filter
- [x] 8.2 Implement an internal event bus/registry that pushes application events to connected `SseEmitter`s
- [x] 8.3 Reject unauthenticated subscription attempts with 401 before opening the stream

## 9. pantheon-web: Project Setup

- [x] 9.1 Scaffold Vite + Vue 3 + TypeScript project with Vue Router and Tailwind CSS configured (`darkMode: 'class'`)
- [x] 9.2 Set up base routing structure (e.g., login view, home/dashboard view)
- [x] 9.3 Implement login flow against `pantheon-service` (Google OAuth2 redirect + email/password form), storing the issued session token

## 10. pantheon-web: Theming

- [x] 10.1 Define the design-token palette (steel/graphite neutrals, blueprint-blue primary, safety-orange accent) evoking technology and civil construction
- [x] 10.2 Implement a theme toggle component switching between dark and light mode
- [x] 10.3 Persist theme preference (e.g., `localStorage`) with `prefers-color-scheme` as the initial fallback

## 11. pantheon-web: SSE Client

- [x] 11.1 Implement a `useSse()` composable wrapping `EventSource`, authenticated against `pantheon-service`'s `/api/sse/subscribe`
- [x] 11.2 Implement automatic reconnect with backoff on connection drop
- [x] 11.3 Reflect received events in UI state as a visible demonstration (e.g., a live event log/notification panel)

## 12. Containerization & Kubernetes Readiness

- [x] 12.1 Write a multi-stage Dockerfile for `pantheon-service`
- [x] 12.2 Write a multi-stage Dockerfile for `pantheon-message`
- [x] 12.3 Write a multi-stage Dockerfile for `pantheon-web` (static build served via a lightweight web server)
- [x] 12.4 Add baseline Kubernetes manifests (Deployment, Service, ConfigMap) per service under `infra/k8s/`, wired to the Actuator health/readiness endpoints for the two Java services

## 13. End-to-End Verification

- [x] 13.1 Bring up the full stack via `infra/docker-compose.yml`
- [x] 13.2 Verify Google OAuth2 login and email/password login both succeed against `pantheon-service` and return a usable session token
- [x] 13.3 Verify a `pantheon-service` action publishes to RabbitMQ and `pantheon-message` consumes and persists it, authenticated via the API token
- [x] 13.4 Verify `pantheon-web` connects to `pantheon-service`'s SSE stream and reflects a pushed event in the UI in real time
- [x] 13.5 Verify dark/light theme toggle works and persists across a page reload in `pantheon-web`
