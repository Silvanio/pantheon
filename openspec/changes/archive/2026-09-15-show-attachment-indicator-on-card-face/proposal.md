## Why

The Tasks board card face already shows a discreet comment-count icon, but nothing hints that a card has files attached — a member has to open every card to find out. The user asked for a small icon next to the comment icon indicating a card has at least one attachment.

## What Changes

- `pantheon-service` computes and exposes an `attachmentCount` per card, on the task board response and on every `task-card-updated` real-time event (mirroring how `commentCount` already works).
- Every place an attachment's link to a task changes — uploading from the task's attach flow, deleting a file directly in Projetos, deleting (or cascade-deleting via a folder) a file that happens to be task-linked — now publishes a `task-card-updated` event for the affected card(s), so the badge stays correct for every viewer in real time, not just the one who acted.
- `pantheon-web`'s card face shows a small paperclip icon next to the comment icon whenever `attachmentCount > 0`; attaching a file from the card detail modal refreshes the local board immediately (same pattern already used after posting a comment).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `obra-tasks-board`: card face gains an attachment indicator; real-time sync of in-place card edits and the attachments-related requirements now cover attachment-count changes.

## Impact

- **Backend**: `SiteDocumentProjectAttachmentRepository` (batch count query), `TaskCardService` (board assembly, `publishCardUpdated` payload, new `attachmentCountForCard`), `TaskCardResponse`/`TaskCardController`, `SiteDocumentProjectService` (now depends on `TaskCardService` to publish updates after upload/delete/folder-cascade-delete affecting a task-linked file).
- **Frontend**: `TaskCard` type, SSE event interfaces/handlers, and the card face template in `TasksBoardPanel.vue`.
- No new endpoints, no migration — purely an additional derived field plus more places that trigger the existing real-time event.
