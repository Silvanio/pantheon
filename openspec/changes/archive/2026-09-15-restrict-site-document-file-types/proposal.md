## Why

The just-shipped Projetos folder explorer accepts any non-empty file of any content type. The user wants to tighten this now, before it sees real use: only image/document/short-video types relevant to a construction site (photos, plans, short clips), and specifically a duration cap on video so someone can't upload a long screen recording or a full walkthrough video that would bloat storage.

## What Changes

- `pantheon-service` restricts every file uploaded into a site's Projetos explorer (both the direct upload path and the task-attach path, since they share the same storage code) to five extensions: `.jpg`, `.jpeg`, `.png`, `.pdf`, `.mp4` — checked case-insensitively against the uploaded file's original name. Anything else is rejected with a clear error.
- `.mp4` uploads are additionally capped at 60 seconds of duration, read directly from the file's `moov`/`mvhd` box (no external process or new dependency). A file whose duration can't be determined (not a valid/parseable MP4 structure) is rejected rather than assumed short.
- `pantheon-web`'s upload controls (Projetos toolbar and the task "Anexar arquivo" flow) restrict the native file picker to these extensions via `accept`, and surface the new rejection messages.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `site-document-projects`: "File upload" requirement narrows from "any content type" to the five allowed extensions, and adds the MP4 duration cap.

## Impact

- **Backend**: `SiteDocumentProjectService.storeFile` (extension allowlist + MP4 duration check); a new small, dependency-free MP4 box parser; no new Maven dependency.
- **Frontend**: `SiteDocumentProjectsPanel.vue` and `TasksBoardPanel.vue` upload inputs (`accept` attribute); `pt-BR.json` error copy.
- No data model or endpoint shape changes — purely a tightened validation rule on the existing upload endpoints.
