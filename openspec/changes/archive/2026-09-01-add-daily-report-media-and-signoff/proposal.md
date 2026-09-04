## Why

The reference competitor's remaining features — photos/videos, attachments, signatures, and PDF export — are what make a daily report a shareable, auditable record instead of just internal data entry. This change adds them on top of `add-daily-construction-report`, and is split out separately because it is the only piece of this whole initiative that needs new infrastructure (file storage). As directed, uploaded files are never stored in the relational database — only their reference is; the files themselves live in an object storage service, organized under a structured key path per report (functionally equivalent to a folder per daily report), not as database blobs.

## What Changes

- Add photo/video upload to a daily report, stored as `DailyReportMedia` records (type, storage key, optional caption) — the file itself goes to object storage, never into Postgres.
- Add document attachments to a daily report, stored as `DailyReportAttachment` records (storage key, original filename) — same file-not-in-database rule.
- Add report sign-off: designated responsible parties (project members) can record a `DailyReportSignature` (approval) against a submitted (`SUBMITTED`) report. This is a recorded approval action (name, role/function, timestamp), not a cryptographic e-signature.
- Add PDF export: generate a downloadable PDF snapshot of a submitted report (all sections + media thumbnails + list of signatures).
- Introduce object storage (S3-compatible) to `platform-foundation`'s local orchestration and container config, since photos/videos/attachments/PDFs need a durable file store outside the database.

## Capabilities

### New Capabilities
- `daily-report-media-and-signoff`: media/attachment upload, sign-off recording, and PDF export for a daily report.

### Modified Capabilities
- `platform-foundation`: adds an object storage service (S3-compatible) to the local orchestration setup and to each service's externalized configuration, needed to persist uploaded media/attachments/PDFs as files rather than database rows.

## Impact

- **Affected code**: `pantheon-service` (new `DailyReportMedia`, `DailyReportAttachment`, `DailyReportSignature` entities/repositories/services/controllers/DTOs; PDF generation; object storage client integration; new Flyway migrations), `pantheon-web` (media gallery/upload UI, attachment upload UI, sign-off action, PDF download button — copy sourced from the `pt-BR` locale resource file), `infra` (object storage service added to `docker-compose.yml`, storage env vars added to Dockerfiles/K8s ConfigMaps).
- **Depends on** `add-daily-construction-report` (a report must exist and be submittable before it can carry media/signatures/a PDF).
- **New REST surface**: `POST /api/daily-reports/{id}/media`, `GET /api/daily-reports/{id}/media`, `GET /api/daily-reports/{id}/media/{mediaId}/content`, `POST /api/daily-reports/{id}/attachments`, `GET /api/daily-reports/{id}/attachments`, `GET /api/daily-reports/{id}/attachments/{attachmentId}/content`, `POST /api/daily-reports/{id}/signatures`, `GET /api/daily-reports/{id}/signatures`, `GET /api/daily-reports/{id}/pdf`. The two `.../content` endpoints and a `contentType` field on both `DailyReportMedia`/`DailyReportAttachment` weren't in the original design — they turned out to be necessary for the gallery/download UI to actually render/serve the uploaded files with the right MIME type.
- **Non-goals**: legally-binding digital signatures (certificate-based), video transcoding/compression, versioned PDF history (each export is generated on demand from current state, not archived), granular per-file access control beyond "any project member can view".
