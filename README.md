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
