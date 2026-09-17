## Context

The Tasks board already has a `commentCount` field flowing through three paths: the board fetch (`TaskCardService.getBoard`, batched via `TaskCommentRepository.countByCardIdIn`), the create/move/patch response DTOs, and the `task-card-updated` SSE payload (`TaskCardService.publishCardUpdated`). Attachments are owned by a different service (`SiteDocumentProjectService`, from the earlier Projetos redesign) — a `SiteDocumentProjectAttachment` optionally carries a `taskCardId`. Nothing currently tells `TaskCardService` when that link changes.

## Goals / Non-Goals

**Goals:**
- Batch-computed `attachmentCount` on the board fetch, mirroring the existing `commentCount` pattern exactly (same batched-query shape, same map-by-card-id assembly).
- Every mutation that changes whether a task has attachments — attach-from-task, direct delete in Projetos, folder cascade delete — triggers the existing `task-card-updated` broadcast so every viewer's badge stays correct, not just the acting user's.

**Non-Goals:**
- A count badge (Trello-style "2 files") — the user asked for presence, matching the existing comment icon's own restraint (a icon, with count only in its tooltip).
- Any change to how attachments are listed/managed within the card modal — that already works; this only affects the collapsed card face.

## Decisions

### 1. Mirror the `commentCount` plumbing exactly, three-deep

`SiteDocumentProjectAttachmentRepository` gains `countByTaskCardIdIn` (batched, grouped `COUNT`) with a `CardAttachmentCount` projection — the same shape as `TaskCommentRepository.CardCommentCount`. `TaskCardService.getBoard()` builds `attachmentCountByCard` the same way it already builds `commentCountByCard`. `TaskCardResponse` and the `task-card-updated` payload both gain `attachmentCount` alongside `commentCount`. Doing it this way, rather than inventing a different shape, keeps the two derived counts symmetric for anyone reading this code later.

### 2. `TaskCardService` depends on a `SiteDocumentProjectAttachmentRepository`, not on `SiteDocumentProjectService`

`TaskCardService` only needs to *read* attachment counts for the board — a repository dependency, injected directly, matching how it already injects `TaskCommentRepository`/`TaskCardAssigneeRepository` etc. directly rather than through another service.

### 3. `SiteDocumentProjectService` depends on `TaskCardService` to publish updates

The reverse relationship is different: `SiteDocumentProjectService` needs to *trigger* the existing `publishCardUpdated(UUID cardId)` (already `public`, already used by `TaskCommentService`/`TaskLabelService`) after any of:
- `uploadFileFromTask` — the newly attached file's card.
- `deleteFile` — only if the deleted attachment had a `taskCardId`.
- `deleteFolder` — the storage-cleanup step already walks every descendant attachment in memory before the cascading DB delete; the distinct set of non-null `taskCardId`s among them each get a `publishCardUpdated` call after the delete completes.

This is a one-way dependency (`SiteDocumentProjectService` → `TaskCardService`); `TaskCardService` has no reference back, so there is no circular-bean issue for Spring to resolve — confirmed by the full-context `PantheonServiceApplicationTests` passing.

### 4. Local board refresh on attach, not just SSE echo

Following the same defensive pattern `onAddComment` already uses (an explicit reload after the mutation, rather than relying solely on the SSE round-trip reaching the acting user's own browser), `onConfirmAttach` now calls the existing `refreshSelectedCard()` helper after a successful attach, so the badge appears immediately for the person attaching the file too.

## Risks / Trade-offs

- **One more repository dependency injected into `TaskCardService`.** Acceptable — it already directly injects seven repositories; this is consistent with the existing style rather than a new pattern.
- **`deleteFolder`'s cascade can now trigger multiple `task-card-updated` broadcasts in one request** (one per distinct card among the purged attachments). Bounded by how many distinct cards had files in the deleted subtree — realistically small, and each broadcast is cheap (same mechanism already used for every other in-place card edit).
