## Why

Every Projetos file (only JPG/JPEG/PNG/PDF/MP4 are accepted, since the previous change) and every task attachment currently only supports downloading — clicking a file saves it to disk instead of showing it. For quick visual checks (a site photo, a plan, a short clip) that's an unnecessary round trip through the OS file system.

## What Changes

- Clicking a file's name/icon in the Projetos explorer now opens an in-app preview (image, PDF, or video, matching what the file actually is) instead of downloading it. The existing download icon is kept, unchanged, as a separate explicit action.
- The same change applies to a task card's "Anexos" list in the detail modal: clicking an attachment previews it; a small download icon (new, matching the Projetos row pattern) remains for explicit download.
- Preview type is determined primarily by the file's stored content type, falling back to its extension (covers a file whose content type was recorded generically) — never opens a browser download prompt for the three supported preview kinds.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `site-document-projects`: "Projetos folder-explorer UI" gains preview-on-click behavior.
- `obra-tasks-board`: "Card attachments section in the detail modal" gains the same preview-on-click behavior.

## Impact

- **Frontend only**: `SiteDocumentProjectsPanel.vue` and `TasksBoardPanel.vue` (new preview modal, icon-kind-by-extension fallback, a dedicated video icon); `pt-BR.json` (a couple of new labels). No backend or data model change — previews reuse the existing authenticated download endpoints, fetched as a blob and rendered in-place instead of saved to disk.
