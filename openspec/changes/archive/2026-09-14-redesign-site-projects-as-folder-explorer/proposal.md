## Why

The "Projetos" tab is a flat, two-level accordion (a list of projects, each expanding to a flat list of PDF-only attachments) with no visual polish. It cannot represent how a real construction site organizes its documentation — nested folders per discipline/stage, mixed file types — and it is not obvious to navigate for a client browsing a large obra. It also has no way to relate a document to the task that produced it. The tab needs to become a proper Dropbox-like file explorer: folders that can contain folders and files to any depth, clear breadcrumb navigation, file metadata (who/when), and a deliberate (one-directional) link to tasks.

## What Changes

- **BREAKING**: `SiteDocumentProject` becomes a self-referencing folder node (`parentId`, nullable = root) instead of a flat per-site list; `SiteDocumentProjectAttachment` becomes a file node that can sit at the root of the site or inside any folder, and is no longer restricted to PDF.
- Folders can be nested to any depth: a folder can contain child folders and files, recursively. Nothing is called "sub-projeto" — a child is simply another "Projeto" (folder) with a `parentId`.
- Folders track `createdAt`/`createdBy` and `updatedAt`/`updatedBy` (renaming or changing its linked task counts as an update); list/detail responses resolve these to display names, not raw ids.
- A folder can optionally link to one `TaskCard` on the same site, set from within the Projetos screen itself. Deleting that task clears the link (`SET NULL`) but never deletes the folder.
- A **separate, one-directional** path lets a file be attached to a task: from the task's own detail view, the user uploads a file and picks a destination folder (or the site's root) from the Projetos tree; the resulting file is stored under that folder and tagged with the task's id. This is not available from within the Projetos screen — a file only gains a task link by being attached from the task. Deleting the task clears the file's task link but never deletes the file.
- `pantheon-web`'s Projetos tab is rewritten as a folder-explorer UI: breadcrumb navigation, combined folder+file listing per level, create-folder, rename-folder, upload/download/delete file, delete folder (cascades to its whole subtree, including object storage cleanup), and a lightweight in-level name filter.
- The task detail view (inside `TasksBoardPanel.vue`) gains an "Anexos" section: a folder-picker + upload control, and a list of files already linked to that card (with their folder path) that can be downloaded directly.
- Direct folder/file management in Projetos still requires `MANAGE` on `DOCUMENT_PROJECTS` (create/rename/delete/upload); attaching a file from a task requires `MANAGE` on `TASKS` plus at least `VIEW` on `DOCUMENT_PROJECTS` (to browse and pick a destination folder) — a deliberate, narrower gate than direct Projetos uploads, since this path is a task action, not a Projetos action.

## Capabilities

### New Capabilities
(none — this reshapes existing capabilities)

### Modified Capabilities
- `site-document-projects`: folder nesting, file/folder metadata and display names, folder-level task linking, arbitrary file types, rename/delete, and the new folder-explorer UI requirements.
- `obra-tasks-board`: adds the task-attachment upload flow (folder-picker upload from the task detail view, and listing a card's linked files) and clarifies that deleting a card unlinks — never deletes — any folder or file that referenced it.

## Impact

- **Backend**: `SiteDocumentProject`/`SiteDocumentProjectAttachment` entities, repositories, `SiteDocumentProjectService`, `SiteDocumentProjectController`, their DTOs and exceptions; a new Flyway migration; `TaskCardController` gains attachment endpoints backed by `SiteDocumentProjectService`; `StorageKeys` gains a root-level-file key variant.
- **Frontend**: `SiteDocumentProjectsPanel.vue` rewritten; `useSiteDocumentProjects.ts` rewritten for tree navigation; `TasksBoardPanel.vue` gains an attachments section and a folder-picker; `pt-BR.json` locale entries rewritten/added.
- No other capability's data model changes; `PermissionCapability.DOCUMENT_PROJECTS` and `TASKS` gating rules are reused as-is (just applied to more endpoints).
