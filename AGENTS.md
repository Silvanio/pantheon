# AGENTS.md

Instructions for AI coding agents working in this repository.

## What this is

Pantheon is a construction-management platform (projects, construction sites,
architectural design records, daily construction reports, equipment/material
registries, material request workflows). It's a monorepo of three
independently deployable services — see [README.md](README.md) for the stack,
ports, and local run instructions.

## Spec-driven workflow (OpenSpec)

This repository uses [OpenSpec](openspec/) as the source of truth for intended
behavior, ahead of code:

- `openspec/specs/` — current, accepted specifications, one directory per
  capability (e.g. `construction-site-management`, `material-request-workflow`).
- `openspec/changes/` — in-flight change proposals; each is a self-contained
  proposal (problem, design, tasks) for a not-yet-built or in-progress change.
- `openspec/changes/archive/` — completed changes, kept for history.

Before implementing a non-trivial feature or behavior change, check whether it
is already described (or contradicted) by a spec in `openspec/specs/`. For new
features, prefer proposing a change under `openspec/changes/` before writing
code, using the `openspec-propose` skill/workflow, and implement against an
approved change's tasks (`openspec-apply-change`). Don't hand-edit
`openspec/specs/` directly — specs are synced from an implemented change
(`openspec-sync-specs`) or updated via `openspec-update-change`.

## Repository layout

```
pantheon-service/   Core REST API (Java 17, Spring Boot) — com.pantheon.service
pantheon-message/   Messaging/notification worker (Java 17, Spring Boot) — com.pantheon.message
pantheon-web/       Frontend (Vue 3, TypeScript, Vite)
infra/              docker-compose.yml, Kubernetes manifests (infra/k8s), shared Maven parent
openspec/           Specs and change proposals (see above)
```

Both Java services follow a layered package structure under their root
package: `controller` (or `web`) → `service` → `repository`, plus `entity`/
`domain`, `dto`, `security`, `messaging`, and `exception`. Keep new code
consistent with this layering — don't put persistence or HTTP concerns
directly in domain entities, and don't bypass the service layer from
controllers.

## Conventions

- **Config via environment variables.** All service configuration
  (`application.yml`) is externalized through env vars with sensible dev
  defaults (`${VAR:default}`). Never hardcode credentials, hosts, or ports —
  follow the existing pattern when adding new config.
- **Each service builds independently.** A build of one of the three services
  must not require the other two to be present or built first.
- **Local infra is docker-compose-managed.** RabbitMQ, both Postgres
  databases, Mailpit (dev SMTP), and MinIO (S3-compatible storage) run via
  `infra/docker-compose.yml`. Uploaded files belong in object storage
  (MinIO/S3), never as binary DB columns.
- **Health/readiness endpoints are required** on both Java services
  (Spring Boot Actuator) for Kubernetes probes — don't remove or disable
  `management.health.*` without replacing the corresponding K8s probe config
  in `infra/k8s/`.
- **`.claude/settings.local.json` is machine-local** (git-ignored) — put
  reusable, repo-wide permission entries in `.claude/settings.json` instead.

## Before committing

- Run the relevant service's test suite for anything you touched
  (`mvn test` for the Java services; check `pantheon-web/package.json` for the
  frontend's scripts).
- Don't commit `target/`, `dist/`, `node_modules/`, `.env`, or `.DS_Store` —
  these are already covered by `.gitignore` files; don't override them.
