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
