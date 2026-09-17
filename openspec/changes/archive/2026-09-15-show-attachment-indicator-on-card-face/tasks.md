## 1. Backend

- [x] 1.1 `SiteDocumentProjectAttachmentRepository`: add `countByTaskCardIdIn` (batched, grouped count) with a `CardAttachmentCount` projection, mirroring `TaskCommentRepository.CardCommentCount`.
- [x] 1.2 `TaskCardService`: inject `SiteDocumentProjectAttachmentRepository`; add `attachmentCountByCard` to `getBoard()`'s `TaskBoard` record and assembly; add `attachmentCountForCard(cardId)`; include `attachmentCount` in `publishCardUpdated`'s payload.
- [x] 1.3 `TaskCardResponse`/`TaskCardController`: add `attachmentCount`, threaded through the board response, the create/move/due-date/assignee responses (0 for a fresh card), and the controller's `respond()` helper.
- [x] 1.4 `SiteDocumentProjectService`: inject `TaskCardService`; call `publishCardUpdated` after `uploadFileFromTask` (the target card), after `deleteFile` when the deleted attachment had a `taskCardId`, and after `deleteFolder` for each distinct `taskCardId` among the purged attachments.

## 2. Frontend

- [x] 2.1 `useTaskCards.ts`: add `attachmentCount` to the `TaskCard` type.
- [x] 2.2 `TasksBoardPanel.vue`: add `attachmentCount` to `TaskCardCreatedEvent`/`TaskCardUpdatedEvent` and their handlers (created defaults to 0; updated merges from the event).
- [x] 2.3 `TasksBoardPanel.vue`: card face gains a small paperclip icon next to the comment icon, shown when `attachmentCount > 0`, with a tooltip showing the count.
- [x] 2.4 `onConfirmAttach`: call `refreshSelectedCard()` after a successful attach so the local board (and the acting member's own card face) updates immediately.
- [x] 2.5 `pt-BR.json`: add `tasks.attachmentsIndicator` tooltip copy.

## 3. Verification

- [x] 3.1 Backend tests: extended `boardIncludesCommentCountsAndAssigneesPerCard` to also assert `attachmentCountByCard`; added assertions to `uploadFileFromTaskRequiresManageTasksAndVisibleDocumentProjects` and `deleteFolderPurgesStorageForEveryDescendantAttachmentBeforeDeletingTheRow` that `publishCardUpdated` is (or isn't) called as appropriate; added `deletingATaskLinkedFilePublishesCardUpdate` and `deletingAFolderWithTaskLinkedAttachmentsPublishesCardUpdateForEachDistinctCard`.
- [x] 3.2 Full backend suite: `./mvnw -o clean test` → 173/173 passing, including the full-context boot test (confirms no circular Spring bean dependency between `TaskCardService` and `SiteDocumentProjectService`).
- [x] 3.3 Frontend `npm run build` green.
- [x] 3.4 Manual live verification: attached a file to a task card from its detail modal, confirmed the paperclip icon appeared on the card face immediately (no reload); added a comment and confirmed both icons render side by side (attachment icon left, comment icon with count right) with the expected DOM structure and tooltip text. No console errors. Test data cleaned up afterward.
