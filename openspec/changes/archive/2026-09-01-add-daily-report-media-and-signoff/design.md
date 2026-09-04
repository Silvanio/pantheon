## Context

`add-daily-construction-report` gives a report a draft/submitted lifecycle but no way to attach evidence (photos, documents) or record who signed off on it, and no way to hand the report to someone outside the platform (a PDF). None of Pantheon's services currently talk to an object store — everything so far is relational data in Postgres. Per explicit product direction, uploaded files are stored as files, not database rows — this change is where that rule first applies.

## Goals / Non-Goals

**Goals:**
- Let media/attachments be uploaded against any report (draft or submitted — evidence often gets added while the report is still being filled).
- Let sign-off only be recorded once a report is `SUBMITTED` (mirrors "assinaturas: define responsáveis por validar e aprovar" as the final step).
- Generate a PDF on demand from a submitted report's current state.
- Store every uploaded file (photo, video, attachment, and the generated PDF if ever cached) as an object in object storage, organized under a predictable, folder-like key path — never as binary data in Postgres.
- Introduce the minimum object storage needed, matching the project's existing "config via env vars, container-ready" convention from `platform-foundation`.

**Non-Goals:**
- Real e-signature/certificate-based signing — a signature here is a recorded approval action by an authenticated member, not a cryptographically verifiable signature.
- Editing/removing media, attachments, or signatures after they're added (append-only for this change).
- PDF archival/versioning — `GET /api/daily-reports/{id}/pdf` always renders the current state; no history of past exports is kept.
- Multiple storage backend choices — pick one (MinIO locally, S3-compatible in any environment) and standardize on it rather than making it pluggable.

## Decisions

### Entity model
```
DailyReportMedia                         -- table: daily_report_media
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  type              VARCHAR    -- PHOTO | VIDEO
  storage_key       VARCHAR    -- object storage key/path, see "Storage key layout" below
  caption           VARCHAR NULL
  uploaded_by       UUID FK app_user.id
  created_at

DailyReportAttachment                    -- table: daily_report_attachment
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  storage_key       VARCHAR
  original_name     VARCHAR
  uploaded_by       UUID FK app_user.id
  created_at

DailyReportSignature                     -- table: daily_report_signature
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  membership_id     UUID FK project_membership.id
  signed_at         TIMESTAMPTZ
```
Files themselves live in object storage, keyed by `storage_key`; only the reference (and small metadata: caption, original filename, uploader) is in Postgres — the binary content never touches the relational database.

### Storage key layout ("folders" per report)
Object keys are namespaced by a predictable path, so each daily report's files are logically grouped exactly like a folder even though object storage has no real directory structure: `construction-sites/{constructionSiteId}/daily-reports/{dailyReportId}/media/{mediaId}.{ext}` for photos/videos, and `construction-sites/{constructionSiteId}/daily-reports/{dailyReportId}/attachments/{attachmentId}.{ext}` for documents. This gives every report's files a single common key prefix, which is what "guardado em pastas" maps to in an object-storage world (a bucket browser or the MinIO console renders these prefixes as folders).

### Object storage
Add MinIO to `infra/docker-compose.yml` for local development (S3-compatible API, so `pantheon-service` only ever talks the S3 protocol and can point at real S3/S3-compatible storage in other environments via env vars: endpoint, bucket, access key, secret key). `pantheon-service` gains a thin storage client used by the media/attachment/PDF endpoints; uploads go through `pantheon-service` (not direct browser-to-storage) to keep authorization/validation (file type, size limit) server-side for this first version.

*Alternative considered*: a plain filesystem directory (a mounted volume, e.g. `/var/pantheon/uploads/...`) instead of an object storage service — rejected because `platform-foundation` already commits to container/Kubernetes-readiness with no service holding local disk state; a filesystem mount doesn't survive pod rescheduling or scale past one replica without a shared network filesystem, while an S3-compatible object store is the standard cloud-native answer and still satisfies "files, not database rows" — MinIO itself stores objects as files on disk under the hood, so locally this is still, literally, files in folders.

*Alternative considered*: browser-to-storage direct upload via pre-signed URLs — rejected for this change to keep the implementation simple (one request path, no pre-signed URL issuance/expiry to manage); can be introduced later as a performance optimization without changing the data model.

*Alternative considered*: storing files in the database as `BYTEA` — rejected per explicit product direction and because it doesn't scale for photos/videos and works against the "externalized config, container-ready" principle already established in `platform-foundation`.

### Sign-off gating
`POST /api/daily-reports/{id}/signatures` only succeeds when the report is `SUBMITTED`; a project member signs as themselves (their own `ProjectMembership`), recording their construction function at signing time for the PDF's audit trail. No approval/rejection semantics — signing is purely additive endorsement (there's no "reject" action here, distinct from `add-material-request-workflow`'s approve/reject).

*Alternative considered*: a formal multi-party approval workflow (e.g., report isn't "complete" until N specific roles sign) — rejected as over-engineering for this phase; the competitor's own description ("define responsáveis por validar e aprovar") doesn't specify a required-signer list, so this design treats every signature as an independent, optional endorsement.

### PDF generation
Server-side rendering (e.g., an HTML template of the report populated with its data, converted to PDF) triggered synchronously on `GET /api/daily-reports/{id}/pdf`, generated fresh each call rather than cached/stored.

*Alternative considered*: generating and storing the PDF in object storage on submit, serving a cached file thereafter — rejected for this change since it would need cache-invalidation logic whenever new media/signatures are added after submission; on-demand generation is simpler and always reflects current state.

## Risks / Trade-offs

- **[Risk]** Uploads passing through `pantheon-service` add memory/bandwidth load to that service for large video files → **Mitigation**: enforce a per-file size limit at the API layer (e.g., a few tens of MB) for this phase; pre-signed direct uploads are the documented future optimization if this becomes a bottleneck.
- **[Risk]** On-demand PDF generation could be slow for a report with many media entries → **Mitigation**: acceptable for expected report sizes in this phase; revisit with caching if reports grow large.
- **[Risk]** No delete on media/attachments/signatures means mistakes are permanent (append-only) → **Mitigation**: intentional for this phase to keep the report an honest record; a moderation/delete capability can be added later if needed.

## Migration Plan

Additive only: new `daily_report_media`, `daily_report_attachment`, `daily_report_signature` tables via new Flyway migrations; new MinIO service in `docker-compose.yml`; new storage env vars in `pantheon-service`'s Dockerfile/K8s ConfigMap. No existing table/endpoint changes. Rollback: drop the three new tables, remove the storage env vars and MinIO service; `pantheon-service` falls back to lacking these endpoints only.

## Open Questions

- File size/type limits — exact numbers (e.g., "photos up to 10MB, videos up to 100MB, jpg/png/mp4 only") need product input before implementation; this design assumes reasonable limits will be set in `application.yml` as configuration, not hardcoded.
- Should a `MEMBER` be able to sign, or only `ADMIN`/certain functions (`ENGINEER`, `SITE_FOREMAN`, `CLIENT`)? This design assumes any project member of that site can sign (self-attested endorsement); revisit if sign-off needs to be restricted to specific functions.
