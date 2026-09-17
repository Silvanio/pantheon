## 1. Migration and entities

- [x] 1.1 Flyway `V53__add_folder_nesting_to_site_document_project.sql`: add `parent_id` (self-FK, `ON DELETE CASCADE`), `task_card_id` (FK to `task_card`, `ON DELETE SET NULL`), `updated_at`, `updated_by` to `site_document_project`; backfill `updated_at = created_at`, `updated_by = created_by`; add indexes on `parent_id` and `task_card_id`.
- [x] 1.2 Same migration: add `construction_site_id` (FK to `construction_site`) and `task_card_id` (FK to `task_card`, `ON DELETE SET NULL`) to `site_document_project_attachment`; backfill `construction_site_id` by joining through `site_document_project_id`; make `site_document_project_id` nullable (re-pointing its FK to `ON DELETE CASCADE`); add indexes on `site_document_project_id` and `task_card_id`.
- [x] 1.3 Update `SiteDocumentProject` entity: add `parentId`, `taskCardId`, `updatedAt`, `updatedBy` fields and getters; add `rename(...)` and `linkTask(...)` mutators.
- [x] 1.4 Update `SiteDocumentProjectAttachment` entity: add `constructionSiteId`, `taskCardId` fields; make `siteDocumentProjectId` nullable.
- [x] 1.5 `SiteDocumentProjectRepository`: added `findChildren` (null-safe root/parent listing) and `findByParentId` (subtree walk).
- [x] 1.6 `SiteDocumentProjectAttachmentRepository`: added `findChildren`, `findBySiteDocumentProjectIdIn` (storage cleanup before cascading folder delete), `findByTaskCardId`.

## 2. Backend service and DTOs

- [x] 2.1 `StorageKeys`: added `siteDocumentRootAttachmentKey` for root-level files.
- [x] 2.2 Rewrote `SiteDocumentProjectService`: `createFolder`, `renameFolder`, `setFolderTaskLink`, `clearFolderTaskLink`, `deleteFolder` (collects descendant attachment storage keys first, purges from `StorageService`, then deletes the folder row and relies on DB cascade), `uploadFile`, `deleteFile`, `getFileContent`, `listContents`, `getBreadcrumbs`, `listAttachmentsForTaskCard`, `folderPathFor`. Read methods use `requireVisible(DOCUMENT_PROJECTS)`; direct mutations use `requireManage(DOCUMENT_PROJECTS)`.
- [x] 2.3 Added `uploadFileFromTask` on `SiteDocumentProjectService`: resolves the card's site, calls `requireManage(TASKS)` + `requireVisible(DOCUMENT_PROJECTS)`, then stores the file with `taskCardId` set.
- [x] 2.4 Added `displayNamesFor` (same pattern as `TaskCommentService.authorNamesFor`) and `taskTitlesFor` helpers, used by controllers to resolve names.
- [x] 2.5 Updated `SiteDocumentProjectResponse` and `SiteDocumentProjectAttachmentResponse`; added `SiteDocumentFolderContentsResponse` and `SiteDocumentBreadcrumbResponse`.
- [x] 2.6 Updated `SiteDocumentProjectRegistrationRequest` to `{name, parentId?, taskCardId?}`; added `RenameSiteDocumentProjectRequest` and `SetFolderTaskLinkRequest`.
- [x] 2.7 Dropped the `"application/pdf"` content-type check from upload; kept the empty-file `InvalidFileException`; added a content-type/original-name fallback for browsers that omit them.

## 3. Backend controllers

- [x] 3.1 `SiteDocumentProjectController`: replaced the old flat endpoints with `GET .../projects/contents`, `GET/PATCH/PUT/DELETE .../site-projects/{id}...`, `POST .../projects/attachments`, `GET/DELETE .../site-project-attachments/{id}...`.
- [x] 3.2 `TaskCardController`: added `POST /api/task-cards/{cardId}/attachments` and `GET /api/task-cards/{cardId}/attachments`.
- [x] 3.3 `ConstructionExceptionHandler`: added `SiteDocumentAttachmentNotFoundException` → 404; `InvalidFileException` already mapped globally (reused, no change needed).

## 4. Frontend composable and shared folder-picker

- [x] 4.1 Rewrote `useSiteDocumentProjects.ts` with `SiteDocumentFolder`/`SiteDocumentFile` types and the full CRUD + navigation API.
- [x] 4.2 Added `listCardAttachments`, `attachFileToCard`, `getCardAttachmentContentBlob` to `useTaskCards.ts`.

## 5. Frontend: Projetos folder-explorer UI

- [x] 5.1 Rewrote `SiteDocumentProjectsPanel.vue` as a folder explorer: breadcrumb bar, combined folder+file listing, "Nova pasta", multi-file "Enviar arquivo", same-level name filter.
- [x] 5.2 Per-row actions gated on `canManage` (passed down from `SiteDetailView.vue`'s resolved `DOCUMENT_PROJECTS` access): rename, delete (with confirmation), and a "Vincular task" picker sourced from the site's task board.
- [x] 5.3 Row display: folder/file type icon (PDF/image/generic), name, last-updated date, last-updated-by display name.
- [x] 5.4 Visual pass done using the existing Tailwind `card`/`btn-*`/`field-*` classes, `blueprint`/`steel` palette, hover-revealed row actions, and an empty-state illustration.

## 6. Frontend: task attachments

- [x] 6.1 Added an "Anexos" section to `TasksBoardPanel.vue`'s card detail modal, loaded in `openCard` alongside comments.
- [x] 6.2 Added an upload control (gated on `canManageTasks && canViewProjects`, both passed down from `SiteDetailView.vue`) that opens a folder-picker (breadcrumb + list, reusing `listContents`/`getBreadcrumbs`) before calling `attachFileToCard`.

## 7. Locale

- [x] 7.1 Replaced the `siteDocumentProjects` block in `pt-BR.json` with the new explorer's keys.
- [x] 7.2 Added `tasks.attachments` block for the card detail modal's new section.

## 8. Verification

- [x] 8.1 Backend: added `SiteDocumentProjectServiceTest` (19 tests) covering folder creation/nesting (root, nested, cross-site rejection), rename, cascading delete with storage cleanup, folder task-link set/clear, file upload (root/nested, non-PDF accepted, empty rejected), file delete, `listContents` at root/nested, `getBreadcrumbs`, `folderPathFor`, and the task-attachment path's dual permission gate (`MANAGE(TASKS)` + `VISIBLE(DOCUMENT_PROJECTS)`, each rejecting independently when missing).
- [x] 8.2 The `TaskCard` deletion → folder/attachment unlink behavior is a database-level `ON DELETE SET NULL` constraint (not application code), so it isn't unit-testable with the existing Mockito-only test setup; verified instead by live manual testing (see 8.5) plus direct SQL confirmation after the test run.
- [x] 8.3 Full backend suite: `./mvnw -o clean test` → 162/162 passing.
- [x] 8.4 Frontend: `npm run build` → typecheck + build green.
- [x] 8.5 Manual live verification performed this session (registered a throwaway QA account/company/site in the local dev environment): created a root folder, a nested subfolder, uploaded a PDF directly into Projetos, renamed a folder, linked a folder to a task card, attached a second file to that same task card via the folder-picker (navigating into a nested folder), confirmed it appeared in both the task's "Anexos" list (with folder path) and the Projetos tree, deleted the linked task card and confirmed — both in the UI and via a direct DB query — that the folder and its files survived with their task links cleared, then deleted the folder and confirmed the cascading delete removed every nested folder/file row (DB-verified). No console errors throughout.
