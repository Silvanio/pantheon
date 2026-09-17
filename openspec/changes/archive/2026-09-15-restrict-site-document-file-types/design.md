## Context

`SiteDocumentProjectService.storeFile` (shared by the direct Projetos upload and the task-attach upload) currently only rejects empty files. The user asked, in two follow-up messages, to (1) limit uploads to JPG/JPEG/PNG/PDF/MP4 and (2) cap MP4 to "short videos" — no exact duration was given, so a default had to be chosen.

## Goals / Non-Goals

**Goals:**
- Reject any upload whose file extension isn't one of `.jpg`, `.jpeg`, `.png`, `.pdf`, `.mp4`.
- Reject an `.mp4` upload longer than a fixed "short video" cap.
- Keep the check dependency-free and fast (runs on every upload, in-memory).

**Non-Goals:**
- Re-encoding, transcoding, or thumbnailing video/images.
- Validating that a file's *content* actually matches its extension (e.g. a renamed `.exe` saved as `.png`) — this is a UX-level allowlist, not a security content-sniffing feature; deeper validation is out of scope for this change.
- A configurable/admin-editable duration limit — it's a fixed constant for now, changeable in code if the need arises.

## Decisions

### 1. Validate by file extension, not `Content-Type`

The user described the limit in terms of file types users recognize (JPG, PNG, PDF, MP4), and multipart `Content-Type` from browsers is inconsistently reliable (some send `application/octet-stream` for less common associations). Extension-based checking, case-insensitive, on the already-existing `extensionOf(originalFilename)` helper is simpler and matches the user's own framing. `.jpg` and `.jpeg` both map to the same underlying image type and are both accepted, listed separately since the user named both.

### 2. MP4 duration cap: 60 seconds, read from the `mvhd` box, no new dependency

MP4/ISO-BMFF files store overall duration in the `moov` container's `mvhd` box: version byte, then either 32-bit or 64-bit `timescale`/`duration` fields depending on that version. A new `Mp4DurationReader` utility walks the top-level box list (4-byte size + 4-byte type, recursing into `moov`) purely over the in-memory `byte[]` already produced by `readBytes(file)` — no external process (`ffprobe`), no new Maven dependency, no native code. `duration_seconds = duration / timescale`.

60 seconds is a judgment call (no duration was specified by the user) — short enough to keep storage bounded for a "prova rápida" style clip, long enough for a walkthrough of a small area. It is a single constant (`MAX_VIDEO_DURATION_SECONDS`) in `SiteDocumentProjectService`, easy to change later.

If the `moov`/`mvhd` box can't be found or parsed (malformed file, or an exotic MP4 variant this minimal parser doesn't handle — e.g. a fragmented/streaming MP4 with duration only in `mehd`), the upload is rejected rather than silently accepted: we can't call something "short" if we can't measure it.

### 3. Same check for both upload paths

Both `uploadFile` (direct Projetos upload) and `uploadFileFromTask` (task attach) call the same private `storeFile`, so putting the check there — rather than duplicating it in each public method — covers both automatically and can't drift out of sync.

## Risks / Trade-offs

- **The 60-second figure is a default, not a number the user specified.** Called out explicitly here and in the summary so it's easy to correct.
- **A hand-rolled MP4 box parser**, not a maintained library. Scoped deliberately narrow (only needs to reach one box, `moov > mvhd`) to keep it small and auditable; unsupported/exotic MP4 structures fail closed (rejected) rather than silently passing.
