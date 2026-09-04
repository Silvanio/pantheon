# platform-foundation Specification

## Purpose

Monorepo structure, per-service local run/build setup, and containerization/Kubernetes-readiness (Dockerfiles, health/readiness probes, externalized config) shared across the `pantheon-service`, `pantheon-message`, and `pantheon-web` services.

## Requirements

### Requirement: Monorepo layout with independently deployable services
The repository SHALL host `pantheon-service`, `pantheon-message`, and `pantheon-web` as independent projects within a single monorepo, each buildable and deployable on its own without requiring the others to be built.

#### Scenario: Single service builds in isolation
- **WHEN** a build is run for only one of the three service directories
- **THEN** that build completes successfully without requiring the other two services to be present or built

### Requirement: Local development orchestration
The repository SHALL provide a local orchestration setup (e.g., docker-compose) that runs RabbitMQ, the required databases, and all three services together for end-to-end local development and testing.

#### Scenario: Full stack starts locally
- **WHEN** a developer runs the local orchestration setup
- **THEN** RabbitMQ, the databases, `pantheon-service`, `pantheon-message`, and `pantheon-web` all start and can communicate with each other

### Requirement: Containerization
Each of the three services SHALL have a Dockerfile that builds a runnable container image for that service, with configuration supplied via environment variables rather than hardcoded values.

#### Scenario: Service image builds
- **WHEN** the Dockerfile for a given service is built
- **THEN** it produces a container image that starts the service successfully when run with the required environment variables set

### Requirement: Kubernetes readiness
Each of the three services SHALL expose health and readiness checks and SHALL have baseline Kubernetes manifests (Deployment, Service, ConfigMap) available in the repository, so the services can be deployed as pods without further restructuring.

#### Scenario: Health/readiness endpoint available
- **WHEN** a container for a Java service (`pantheon-service` or `pantheon-message`) is running
- **THEN** its liveness and readiness endpoints respond with a status usable by a Kubernetes probe

#### Scenario: Baseline manifests present
- **WHEN** the repository's `infra/k8s` directory is inspected
- **THEN** it contains a Deployment, Service, and ConfigMap manifest for each of the three services

### Requirement: Object storage for uploaded files
The local orchestration setup SHALL include an S3-compatible object storage service, and each service that reads or writes files SHALL be configured to reach it via externalized environment variables (endpoint, bucket, access key, secret key) rather than hardcoded values. Uploaded files SHALL be stored as objects in this service, organized under a predictable, folder-like key path per owning record — never as binary columns in a relational database.

#### Scenario: Object storage available locally
- **WHEN** a developer runs the local orchestration setup
- **THEN** an S3-compatible object storage service starts alongside RabbitMQ, the databases, and the three application services, and `pantheon-service` can read and write objects to it

#### Scenario: Storage configuration externalized
- **WHEN** `pantheon-service`'s container is inspected
- **THEN** its object storage endpoint, bucket, and credentials are supplied via environment variables, not hardcoded in the image or source

#### Scenario: Files organized by predictable key path
- **WHEN** a file is stored in object storage on behalf of a specific record (e.g., a daily report)
- **THEN** its object key is namespaced under a path prefix scoped to that record, so all of that record's files share a common, folder-like prefix
