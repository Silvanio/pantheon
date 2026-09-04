## ADDED Requirements

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
