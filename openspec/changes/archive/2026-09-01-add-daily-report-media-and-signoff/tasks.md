## 1. Infra: Object storage

- [x] 1.1 Add MinIO service to `infra/docker-compose.yml` (S3-compatible, with a default bucket) — plus a `minio-init` one-shot service that creates the bucket via `mc mb`, since MinIO doesn't auto-create buckets
- [x] 1.2 Add object storage env vars (endpoint, bucket, access key, secret key) to `pantheon-service`'s `.env.example`, Dockerfile, and K8s ConfigMap — access/secret key went into the K8s Secret, not the ConfigMap, matching the existing pattern for other credentials
- [x] 1.3 Add an S3-compatible client dependency and a thin `StorageService` wrapper (put/get object) to `pantheon-service`, encapsulating the report-scoped key path convention (`construction-sites/{siteId}/daily-reports/{reportId}/...`) — AWS SDK v2 `software.amazon.awssdk:s3` (S3-protocol client, works against MinIO with `forcePathStyle`); key layout in `StorageKeys`

## 2. Backend: Media and attachments

- [x] 2.1 Create `daily_report_media` and `daily_report_attachment` tables via Flyway migrations
- [x] 2.2 Add `DailyReportMedia`/`DailyReportAttachment` entities, repositories, service methods (upload, list), DTOs — also added a `content_type` column/field to both (needed to serve files back with the right MIME type; not in the original design, a small necessary addition) and content-serving endpoints (`GET .../media/{mediaId}/content`, `GET .../attachments/{attachmentId}/content`) since the design didn't specify one but the gallery/download UI needs it
- [x] 2.3 Add controller endpoints: `POST/GET /api/daily-reports/{id}/media`, `POST/GET /api/daily-reports/{id}/attachments`
- [x] 2.4 Enforce file type/size limits (configurable via `application.yml`) — size limits per type (`pantheon.storage.max-photo-size-mb`/`max-video-size-mb`/`max-attachment-size-mb`); "type" is validated by the caller-supplied `MediaType` enum for media, not MIME sniffing
- [x] 2.5 Unit tests for upload and listing (mocking the storage client) — 7/7 green (`DailyReportMediaServiceTest`)

## 3. Backend: Sign-off

- [x] 3.1 Create `daily_report_signature` table via Flyway migration
- [x] 3.2 Add `DailyReportSignature` entity, repository, service method (sign, list), DTOs
- [x] 3.3 Add controller endpoints: `POST/GET /api/daily-reports/{id}/signatures`
- [x] 3.4 Enforce SUBMITTED-only signing
- [x] 3.5 Unit tests: sign a submitted report, reject signing a draft, list signatures — 3/3 green (`DailyReportSignatureServiceTest`)

## 4. Backend: PDF export

- [x] 4.1 Choose and add a server-side HTML-to-PDF (or equivalent) library dependency — `com.openhtmltopdf:openhtmltopdf-pdfbox`
- [x] 4.2 Build a report template covering all sections, media thumbnails, attachments list, and signatures — photos embedded as base64 data URIs; videos shown as a caption placeholder (can't meaningfully embed in a PDF)
- [x] 4.3 Add `GET /api/daily-reports/{id}/pdf` endpoint generating the PDF on demand
- [x] 4.4 Unit/integration test for PDF generation succeeding on a representative report — 1/1 green (`DailyReportPdfServiceTest`), asserts the `%PDF-` magic bytes on a fully-populated report including one photo

## 5. Frontend

- [x] 5.1 Add media gallery with upload to the daily report view, copy sourced from `pt-BR.json` — photos rendered from the new content-serving endpoint via object URLs; videos shown as a labeled placeholder (no inline video player)
- [x] 5.2 Add attachment list with upload to the daily report view, copy sourced from `pt-BR.json`
- [x] 5.3 Add sign-off action (enabled only when `SUBMITTED`) and signature list display
- [x] 5.4 Add PDF download action
- [x] 5.5 Add API client functions for media/attachment/signature/PDF endpoints — `useDailyReports.ts` extended with multipart-upload and blob-fetch helpers

## 6. Verification

- [x] 6.1 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes — full suite green (`DailyReportMediaServiceTest` 7/7, `DailyReportSignatureServiceTest` 3/3, `DailyReportPdfServiceTest` 1/1, plus every prior suite)
- [x] 6.2 Build `pantheon-web` (`npm run build`) and confirm it passes — clean, 0 type errors
- [x] 6.3 Manually exercise: upload a photo and an attachment, submit a report, sign it, download the PDF, and confirm signing a draft report is rejected — exercised end-to-end against a live `pantheon-service` + real MinIO: uploaded a real 1×1 JPEG and a text attachment, confirmed 409 signing the draft, submitted, signed (201), downloaded both files back byte-for-byte correct via the content endpoints, downloaded a valid 1-page PDF (`file` confirms `PDF document, version 1.4`)
- [x] 6.4 Confirm `docker compose up` starts MinIO alongside the existing services, `pantheon-service` can reach it, and uploaded files appear under the report's key-prefix "folder" in the MinIO console — `docker compose up -d minio minio-init` started MinIO healthy and auto-created the bucket; `mc ls --recursive` confirmed both uploaded files under `construction-sites/{siteId}/daily-reports/{reportId}/{media,attachments}/...`, exactly matching the design's key layout
