# Pantheon

Pantheon is a construction-management platform: projects, construction sites and
architectural design records, daily construction reports, equipment/material
registries, and material request workflows, built as an independently
deployable monorepo.

## Services

| Service            | Stack                          | Port | Description                                          |
|--------------------|---------------------------------|------|-------------------------------------------------------|
| `pantheon-service`  | Java / Spring Boot              | 8081 | Core REST API, auth (JWT + Google OAuth2), SSE, domain logic |
| `pantheon-message`  | Java / Spring Boot              | 8082 | Async messaging worker (RabbitMQ consumer, email notifications) |
| `pantheon-web`      | Vue 3 + TypeScript + Vite       | 8585 | Frontend SPA                                          |

Each service builds and runs independently; `infra/` ties them together for local
development and Kubernetes.

## Local development

Requirements: Docker, Java 17+, Maven, Node.js.

Start the shared infrastructure (Postgres x2, RabbitMQ, Mailpit, MinIO):

```bash
cd infra
docker compose up -d
```

This starts:

- `service-db` (Postgres, port 5433) — `pantheon-service` database
- `message-db` (Postgres, port 5434) — `pantheon-message` database
- `rabbitmq` (ports 5672 / 15672 management UI)
- `mailpit` (SMTP on 1025, web UI on 8025) — catches outgoing dev email
- `minio` (ports 9000 / 9001) — S3-compatible object storage for uploads

Then run each service (locally, outside Docker, for fast iteration):

```bash
# pantheon-service
cd pantheon-service && mvn spring-boot:run

# pantheon-message
cd pantheon-message && mvn spring-boot:run

# pantheon-web
cd pantheon-web && npm install && npm run dev
```

Copy `.env.example` to `.env` and adjust as needed — all configuration is
externalized via environment variables (see each service's
`src/main/resources/application.yml`).

Alternatively, `docker compose up -d` in `infra/` (with no service filter) will
also build and run the three application services as containers, using the
Dockerfiles and `infra/k8s/*.yaml` manifests as the basis for Kubernetes
deployment.

## Credential files (e.g. Firebase Admin SDK)

Some config values are a *file* (a service-account JSON key, a certificate) rather
than a plain string, referenced by an env var holding its path — e.g.
`PANTHEON_FIREBASE_CREDENTIALS_PATH` (see `pantheon-mobile/README.md`'s "Activating
push notifications"). These files are never committed; how each environment
supplies one differs:

- **Local (IntelliJ/IDE)**: put the file under `pantheon-service/secrets/` (already
  git-ignored) and set the path env var in your Run/Debug Configuration's
  "Environment variables" field — not in a global shell profile, so it stays
  scoped to that run config and doesn't leak into unrelated shells.
- **Another developer's machine**: nobody shares the same key file. Each developer
  either generates their own (e.g. their own Firebase service-account key, scoped
  to a shared dev project) or gets one issued via the team's secret manager, drops
  it in their own `pantheon-service/secrets/`, and points their own IDE/shell env
  var at it.
- **Plain Docker** (running the service container directly, not just `infra/`'s
  local databases): bind-mount the file as a read-only volume and point the env
  var at the in-container path — never `COPY` it into the image/Dockerfile, which
  would bake the secret into every image layer and the registry:
  ```bash
  docker run -v $(pwd)/pantheon-service/secrets/firebase-adminsdk.json:/secrets/firebase-adminsdk.json:ro \
    -e PANTHEON_FIREBASE_CREDENTIALS_PATH=/secrets/firebase-adminsdk.json ...
  ```
- **Docker Swarm**: use a Docker secret, which Swarm mounts at
  `/run/secrets/<name>` automatically:
  ```bash
  docker secret create firebase-adminsdk-key pantheon-service/secrets/firebase-adminsdk.json
  ```
  then reference it in the service (`secrets: [firebase-adminsdk-key]`) and set
  `PANTHEON_FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-adminsdk-key`.
- **Kubernetes**: a file-based credential needs a `Secret` mounted as a *volume*,
  not `envFrom`/`secretKeyRef` (those only inject string values as env vars). See
  the `pantheon-service-firebase-credentials` Secret + volume mount in
  `infra/k8s/pantheon-service.yaml` for the pattern — populate the real value via
  your secret manager (Vault, cloud KMS, sealed-secrets), never commit it.

## Environment profiles (Local / Dev / PRD) and Sentry

`pantheon-service` and `pantheon-web` both use a formal Local/Dev/PRD environment split (see
`openspec/specs/error-tracking/spec.md`), with Sentry error tracking as the first thing gated by
it — Local never sends anything to Sentry, by construction, not just by default.

- **`pantheon-service`** (Spring profiles): `application-local.yml` / `application-dev.yml` /
  `application-prd.yml` alongside the shared `application.yml`. `SPRING_PROFILES_ACTIVE` defaults
  to `local` when unset, so a bare `mvn spring-boot:run` is always Local. The `local` profile
  hardcodes `sentry.dsn: ""`. The `dev`/`prd` profiles hardcode the real DSN directly (not an env
  var) — a Sentry DSN is a semi-public identifier (like a Firebase Web API key), not a real
  secret, so unlike the Firebase credentials above it needs no env var, no IDE run-config setup,
  and only a plain `ConfigMap` entry for `SPRING_PROFILES_ACTIVE` in Kubernetes (see
  `infra/k8s/pantheon-service.yaml`) — never a `Secret`+volume. To test the `dev`/`prd` profiles
  locally, just set `SPRING_PROFILES_ACTIVE=dev` (or `prd`) in your IntelliJ Run/Debug
  Configuration; nothing else is needed to also enable Sentry.
- **`pantheon-web`** (Vite modes): `.env.development` (Local — Vite's own default `npm run dev`
  mode), `.env.dev` (a custom mode, `npm run build:dev` / `--mode dev`), `.env.production` (PRD
  — Vite's own default `npm run build` mode). Because `VITE_`-prefixed vars are always compiled
  into the client bundle at build time, `.env.dev`/`.env.production` carry the real
  `VITE_SENTRY_DSN` value directly (see `pantheon-web/.env.example`) rather than deferring to a
  runtime env var — there's no meaningful way to keep a client-bundled value "secret" regardless,
  and it isn't one. **Never create a file named `.env.local`** for the Local tier — Vite treats
  that filename specially (always loaded, in every mode, meant for personal gitignored
  overrides), not as a mode-specific file; naming it that would leak `VITE_SENTRY_DSN` into
  Dev/PRD builds too.
- **`pantheon-mobile`**: already had an equivalent (`env/local-simulator.json`,
  `env/local-device.json`, `env/prod.json`, see `pantheon-mobile/README.md`) — `SENTRY_DSN` is
  blank in the two local files and set to the real value in `env/prod.json`.

## Project structure

```
pantheon-service/   Core API service (Spring Boot)
pantheon-message/   Messaging/notification worker (Spring Boot)
pantheon-web/       Frontend (Vue 3 + Vite)
infra/              docker-compose, Kubernetes manifests, shared Maven parent
openspec/           Spec-driven change proposals and specs (see openspec/specs)
```

## Specs

This repository follows a spec-driven workflow via [OpenSpec](openspec/). Current
specs live in `openspec/specs/`; in-flight change proposals live in
`openspec/changes/`. See [AGENTS.md](AGENTS.md) for how AI agents should work in
this repo.
