## Context

Today `SiteDocumentProject` is a flat list per construction site (name, creator, date) and `SiteDocumentProjectAttachment` is a flat list of PDF-only files per project — no nesting, no delete, no rename, raw UUIDs shown for "who", and no relationship to tasks. `pantheon-web`'s `SiteDocumentProjectsPanel.vue` renders this as a two-level accordion. The user wants a Dropbox-like explorer: folders nested to any depth (a folder holds folders and files), a polished navigable UI, audit metadata with resolved display names, an optional folder→task link that survives task deletion, and a *separate* task→file attachment path where the task screen itself uploads a file into a folder the user picks.

TaskCard today has no attachment concept at all (confirmed by direct exploration of the entity, service, and controller) — comments, labels, and assignees are its only related data. `StorageService`/`StorageKeys` (S3/MinIO put/get by key) is the shared, reusable storage abstraction already used for daily-report media, company logos, site photos, and material delivery photos.

## Goals / Non-Goals

**Goals:**
- Arbitrary folder nesting under a construction site, using the *same* entity for every level — no distinct "sub-projeto" concept.
- Files can sit at the site's root or inside any folder, of any content type (not just PDF).
- Folder metadata: created/updated date and user (resolved to a display name), shown in the UI.
- A folder may optionally reference one `TaskCard`; deleting that card clears the reference but never touches the folder.
- A file may optionally reference one `TaskCard`, but *only* ever gets that reference by being uploaded through the task's own "attach file" flow (destination folder chosen from the Projetos tree at attach time) — never by linking from within the Projetos screen.
- A visually polished, easy-to-navigate explorer UI (breadcrumbs, combined folder+file listing, create/rename/delete/upload/download), usable by both company staff and view-only site members (clients).

**Non-Goals (deferred, not in this change):**
- Drag-and-drop move of a folder/file to a different parent after creation (parent is set once, at creation time).
- Whole-tree search (only an in-level name filter on the folder currently open).
- Soft delete / trash / file versioning — deleting is permanent, same as every other hard-delete already in this codebase.
- File or folder previews/thumbnails beyond a type-based icon.
- Renaming a file (only folders can be renamed in this change).

## Decisions

### 1. One self-referencing entity for every folder level — no "sub-projeto"

`SiteDocumentProject` gains a nullable `parent_id` self-FK (`ON DELETE CASCADE`) instead of introducing a new entity for nested levels. A row with `parent_id = null` is a root-level folder of the site. Because a folder's parent is fixed at creation and never changed afterward (no move feature), there is no cycle risk to guard against — the tree can only grow by adding leaves.

### 2. Files can live at the root, so the attachment's project reference becomes nullable

`SiteDocumentProjectAttachment.site_document_project_id` becomes nullable (`null` = file sits at the site's root, sibling to root folders) and the attachment gains its own `construction_site_id` (not null) so root-level files — and every query that needs "this site's files" — don't require a join through a project row that might not exist. `ON DELETE CASCADE` from `site_document_project` still applies when `site_document_project_id` is set, so deleting a folder deletes the file rows nested under it.

The PDF-only restriction (`InvalidFileException` when content-type isn't `application/pdf`) is dropped; any non-empty file is accepted (the existing 100MB Spring multipart limit still applies globally). The attachment's stored `content_type` is used as-is for both storage (`StorageService.putObject`) and download (`Content-Type` response header) instead of the endpoint hardcoding `application/pdf`.

### 3. Folder deletion: collect storage keys before the cascading DB delete

Object storage has no cascading delete. `SiteDocumentProjectService.deleteFolder` first walks the subtree in memory (self + every descendant folder, transitively) to collect every attachment's `storageKey` under it — including root-level siblings are *not* touched, only descendants of the folder being deleted — deletes those objects from `StorageService`, then deletes the folder row itself; the DB's `ON DELETE CASCADE` on both `site_document_project.parent_id` and `site_document_project_attachment.site_document_project_id` removes every descendant folder/file row in one statement. No trash/undo — deletion is immediate and permanent, matching every other delete in this codebase.

### 4. Task links are two independent, one-directional relationships, both `ON DELETE SET NULL`

- `site_document_project.task_card_id` (nullable, FK to `task_card`, `ON DELETE SET NULL`): set/cleared *only* from the Projetos screen (folder create/edit), by a user with `MANAGE` on `DOCUMENT_PROJECTS`. Picking the task reuses the already-loaded task board (`GET /api/construction-sites/{siteId}/task-board`) client-side — no new backend endpoint needed just to list tasks.
- `site_document_project_attachment.task_card_id` (nullable, FK to `task_card`, `ON DELETE SET NULL`): set *only* when the file is uploaded through the task's own attach-file flow; never editable from the Projetos screen.

Using `ON DELETE SET NULL` at the database level means `TaskCardService.deleteCard` needs **no changes at all** — Postgres itself clears both references when a card is deleted, which is exactly "task deletion unlinks, never deletes, the folder/file."

### 5. Permission gate for the task→file attach path: `MANAGE(TASKS)` + `VISIBLE(DOCUMENT_PROJECTS)`, not `MANAGE(DOCUMENT_PROJECTS)`

Attaching a file from a task is deliberately a *task* action (same gate as adding a comment or assignee: `requireManage(..., PermissionCapability.TASKS)`), not a Projetos action — this is the whole point of the separate path the user asked for. But the user must still be able to *see* the Projetos tree to pick a destination folder, so the endpoint also calls `requireVisible(..., PermissionCapability.DOCUMENT_PROJECTS)`: a member whose Projetos access is `HIDDEN` cannot use this path either, keeping `HIDDEN` meaningful. A concrete consequence: `SITE_FOREMAN` (default `MANAGE` on `TASKS`, `VIEW` on `DOCUMENT_PROJECTS`) can attach files to tasks even though they cannot upload directly from the Projetos tab — documented here as intentional, not an oversight.

Everything else (list/browse folders and files, download, create/rename/delete folder, upload/delete file directly in Projetos, set/clear a folder's task link) keeps the existing `requireVisible`/`requireManage` split on `PermissionCapability.DOCUMENT_PROJECTS` that the rest of this capability already uses.

### 6. API shape: one "contents of a folder" listing endpoint, per-item endpoints for everything else

Dropbox-style navigation loads one level at a time, so the read path is a single combined endpoint rather than separate folder/file lists a client would need to merge:

- `GET /api/sites/{siteId}/projects/contents?parentId={uuid?}` → `{ folders: [...], files: [...] }` for that parent (root when `parentId` omitted). Requires `VISIBLE(DOCUMENT_PROJECTS)`.
- `GET /api/site-projects/{id}/breadcrumbs` → ordered `[{id, name}]` from root to (and including) `id`, one round trip for the breadcrumb bar instead of N client-side lookups.
- `POST /api/sites/{siteId}/projects` `{name, parentId?, taskCardId?}` → create folder. `MANAGE(DOCUMENT_PROJECTS)`.
- `PATCH /api/site-projects/{id}/name` `{name}` → rename. `MANAGE(DOCUMENT_PROJECTS)`.
- `PUT /api/site-projects/{id}/task-link` `{taskCardId}` / `DELETE /api/site-projects/{id}/task-link` → set/clear the folder's task link. `MANAGE(DOCUMENT_PROJECTS)`.
- `DELETE /api/site-projects/{id}` → cascading delete (Decision 3). `MANAGE(DOCUMENT_PROJECTS)`.
- `POST /api/sites/{siteId}/projects/attachments?parentId={uuid?}` (multipart `file`) → upload directly into Projetos (root or a folder). `MANAGE(DOCUMENT_PROJECTS)`.
- `GET /api/site-project-attachments/{id}/content` → raw bytes with the *stored* content type (Decision 2) and `Content-Disposition: attachment; filename="..."`. `VISIBLE(DOCUMENT_PROJECTS)`.
- `DELETE /api/site-project-attachments/{id}` → delete one file + its storage object. `MANAGE(DOCUMENT_PROJECTS)`.
- `POST /api/task-cards/{cardId}/attachments?parentId={uuid?}` (multipart `file`) → the task→file attach path (Decision 5); returns the created file with its resolved folder path.
- `GET /api/task-cards/{cardId}/attachments` → files currently linked to this card, each with a resolved folder path (e.g. `"Estrutural / Fundação"` or `null` for root) for display in the task modal.

`SiteDocumentProjectResponse` and `SiteDocumentProjectAttachmentResponse` are extended with resolved `createdByName`/`updatedByName` (same "display name, fallback to email" lookup `TaskCommentService.authorNamesFor` already uses) rather than raw ids, and the folder response includes `linkedTaskTitle` (looked up server-side when `taskCardId` is set) so the UI never needs a second round trip just to label the link.

### 7. Frontend: single rewritten explorer component, reusable navigation composable

`useSiteDocumentProjects.ts` is rewritten around `listContents(siteId, parentId)` / `getBreadcrumbs(id)` plus the CRUD calls above. `SiteDocumentProjectsPanel.vue` becomes a folder-explorer: breadcrumb bar, combined grid/list of folder and file rows (icon by type, name, updated date, updated-by name), toolbar (new folder, upload — multiple files allowed per upload action, looped client-side — and a same-level name filter), and per-row actions (open, rename, delete, download, edit task link for folders). `TasksBoardPanel.vue`'s card-detail modal gains an "Anexos" section reusing the same composable for a lightweight folder-picker (breadcrumb + list, reusing `listContents`/`getBreadcrumbs`, no separate backend endpoint) plus the upload/list calls above.

## Risks / Trade-offs

- **Broad rewrite, not additive**: this is explicitly flagged as a breaking change to `SiteDocumentProject`'s shape (flat → tree) and to the PDF-only rule. There is no dual-write/migration-period concern because the existing data is a strict subset of the new shape (every existing row becomes a root-level folder with no task link; every existing attachment becomes a file with no task link) — the migration backfills `parent_id = null`, `updated_at = created_at`, `updated_by = created_by` for existing rows, and `construction_site_id` on attachments by joining through their (still-required-at-migration-time) project.
- **Cascading hard delete**: deleting a folder with many descendants deletes everything in it, including any files linked to tasks. This is disclosed to the user in the UI's delete confirmation (folder + file counts) rather than blocked — consistent with there being no trash/undo anywhere else in the app.
- **The `MANAGE(TASKS)`-not-`MANAGE(DOCUMENT_PROJECTS)` gate on task attachments (Decision 5) is a narrower check than direct Projetos uploads for the same underlying write.** This is deliberate per the user's explicit request for a separate path, and is called out here so it isn't mistaken for an oversight in review.
