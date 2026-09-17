## 1. Projetos explorer

- [x] 1.1 `SiteDocumentProjectsPanel.vue`: add preview state (`previewFile`, `previewUrl`, `previewLoading`, `previewError`) and `onPreviewFile`/`closePreview`; revoke the object URL on close and on unmount.
- [x] 1.2 Change the file row's primary click to `onPreviewFile` (falls back to download for the unreachable "file" icon kind); keep the existing hover download icon calling `onDownloadFile` unchanged.
- [x] 1.3 Make `fileIconKind` fall back to the filename extension when content type doesn't resolve to pdf/image/video (covers a generic/missing content type for an otherwise-allowed extension).
- [x] 1.4 Add the preview modal: filename header, download + close controls, and `<img>`/`<video controls autoplay>`/`<iframe>` bodies keyed off `fileIconKind`.

## 2. Task attachments

- [x] 2.1 `TasksBoardPanel.vue`: add the same preview state, `attachmentIconKind`, `onPreviewCardAttachment`/`closeAttachmentPreview`; revoke on close, on card-modal close, and on unmount.
- [x] 2.2 Restructure the "Anexos" list row: primary click previews, a new small download icon (hover-revealed, matching the Projetos row pattern) stays available; row now shows a type icon (pdf/image/video/generic) instead of always the generic file icon.
- [x] 2.3 Add the preview modal (same shape as Projetos', stacked above the card detail modal with a higher z-index).

## 3. Locale

- [x] 3.1 `pt-BR.json`: add `siteDocumentProjects.close` and `siteDocumentProjects.previewError` (reused by both panels).

## 4. Verification

- [x] 4.1 Frontend `npm run build` green (typecheck covers both rewritten components).
- [x] 4.2 Manual live verification: uploaded a real PNG, a minimal real PDF, and a synthetic (structurally valid, unplayable) MP4 into Projetos — image preview rendered the actual pixels, PDF preview rendered via the browser's built-in viewer with its toolbar, video preview opened a native `<video>` player with controls. Repeated the image case via a task's "Anexar arquivo" flow and confirmed the preview modal opens correctly stacked above the card detail modal. No console errors. Test files cleaned up afterward.
