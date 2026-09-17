## Context

Both file lists (`SiteDocumentProjectsPanel.vue`'s folder explorer and `TasksBoardPanel.vue`'s card "Anexos" section) already fetch a file's bytes as a `Blob` for download, via `getFileContentBlob`/`getCardAttachmentContentBlob` — the same authenticated endpoints (`GET /api/site-project-attachments/{id}/content`) that set `Content-Disposition: attachment`. That header only matters for a direct browser navigation; it's irrelevant once the response is read via `fetch` into a `Blob` and displayed through an object URL, which is exactly what a preview needs too.

## Goals / Non-Goals

**Goals:**
- Click a file → see it (image, PDF, or video), not a save dialog.
- Keep an explicit, unambiguous download action available everywhere preview is added.
- No backend changes — this is presentation-only, reusing existing endpoints.

**Non-Goals:**
- Thumbnails/lazy preview generation, PDF page navigation beyond the browser's own built-in viewer, video transcoding, or any other-file-type preview (there are no other accepted types after `restrict-site-document-file-types`).

## Decisions

### 1. Reuse the download fetch, render instead of save

`onPreviewFile`/`onPreviewCardAttachment` call the same blob-fetching composable functions already used for download, then `URL.createObjectURL(blob)` into an `<img>`, `<video controls>`, or `<iframe>` (for PDF, relying on the browser's native PDF viewer) inside a modal — instead of creating a synthetic `<a download>` click. The object URL is revoked when the modal closes or the component unmounts, so a viewer isn't left dangling in memory.

### 2. Icon/preview kind resolved from content type, falling back to extension

The existing `fileIconKind` (Projetos) helper checked only `contentType`. A file can end up with a generic/missing content type despite being one of the five allowed extensions (e.g., a browser not recognizing an uncommon MIME association) — so both it and a new equivalent for task attachments (`attachmentIconKind`) now fall back to the filename extension when the content type doesn't match `application/pdf`/`image/*`/`video/*`. Since the backend only ever accepts `jpg`/`jpeg`/`png`/`pdf`/`mp4`, this fallback is exhaustive — a file that reaches either list can always be classified as image, pdf, or video.

### 3. Download stays a separate, explicit control

The user asked to keep the existing download icon working as-is and add preview as the new interaction on click. So the row's primary click target now opens the preview; download is demoted to (kept as) a small icon button, shown on hover in Projetos (existing pattern) and added as a small icon in the task attachments list (previously that list had no separate download affordance — clicking the row was the only way to get the file).

## Risks / Trade-offs

- **PDF preview depends on the browser's built-in PDF viewer** rendering inside an `<iframe>` pointed at a blob URL. This works in all evergreen desktop/mobile browsers Chrome/Edge/Firefox/Safari ship today; there is no fallback for a hypothetical browser without one, but none of this codebase's supported targets lack it.
- **A malformed/undecodable video or image blob** will show the browser's native broken-media state inside the modal rather than a custom error — acceptable since the upload-time validation (extension + MP4 duration check) already rejects non-conforming files before they can reach storage.
