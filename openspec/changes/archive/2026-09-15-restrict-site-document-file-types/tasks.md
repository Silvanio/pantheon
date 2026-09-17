## 1. Backend validation

- [x] 1.1 Added `Mp4DurationReader` utility: parses the top-level ISO-BMFF box list of a `byte[]`, finds `moov` then `mvhd`, and returns duration in seconds (handling both mvhd version 0 and version 1 field widths), or empty if not found/parseable.
- [x] 1.2 `SiteDocumentProjectService`: added an extension allowlist (`jpg`, `jpeg`, `png`, `pdf`, `mp4`, case-insensitive) checked in `storeFile`, rejecting anything else with `InvalidFileException`.
- [x] 1.3 `SiteDocumentProjectService`: added `MAX_VIDEO_DURATION_SECONDS = 60`; for `.mp4` uploads, uses `Mp4DurationReader` and rejects (via `InvalidFileException`) when duration exceeds the cap or can't be determined.

## 2. Frontend

- [x] 2.1 `SiteDocumentProjectsPanel.vue`: added `accept=".jpg,.jpeg,.png,.pdf,.mp4"` to the upload file input; also added a dedicated video icon (previously mp4 fell into the generic file icon).
- [x] 2.2 `TasksBoardPanel.vue`: added the same `accept` to the task "Anexar arquivo" file input.
- [x] 2.3 `pt-BR.json`: updated upload error copy (`siteDocumentProjects.uploadError`, `tasks.attachments.error`) mentioning the allowed types and the 60-second video cap.

## 3. Verification

- [x] 3.1 Backend unit tests: new `Mp4DurationReaderTest` (5 tests: version 0, version 1, no moov box, moov without mvhd, zero timescale) and `SiteDocumentProjectServiceTest` additions (all 5 allowed extensions accepted, disallowed extension rejected, video within cap accepted, video exceeding cap rejected, video with undeterminable duration rejected). Replaced the now-invalid `uploadFileAcceptsNonPdfContentType` test (it uploaded a `.dwg`, no longer accepted).
- [x] 3.2 Full backend suite: `./mvnw -o clean test` → 171/171 passing.
- [x] 3.3 Frontend `npm run build` → typecheck + build green.
- [x] 3.4 Manual live verification in the running dev environment (same QA account/site from the previous change): uploaded a `.txt` file and confirmed rejection with the new error message; uploaded a `.png` and confirmed acceptance with the correct icon; uploaded a synthetic 90-second `.mp4` (built client-side with a real `moov`/`mvhd` box) and confirmed the server rejected it with HTTP 400; uploaded a synthetic 30-second `.mp4` and confirmed it was accepted and rendered with the new video icon. No unexpected console errors (only the two expected failed-upload 400s). Test files cleaned up afterward.
