# site-document-projects Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Folder creation and nesting
`pantheon-service` SHALL allow a member with `MANAGE` access to a construction site's `DOCUMENT_PROJECTS` to create a `SiteDocumentProject` (a folder), recording a name, and optionally a parent folder on the same site. A folder created with no parent SHALL sit at the site's root. A folder MAY itself contain child folders (created the same way, naming their parent) and files, to any depth — a child folder is a `SiteDocumentProject` like any other, never a distinct kind of entity.

#### Scenario: Member creates a root folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` creates a folder with no parent
- **THEN** `pantheon-service` persists it at the construction site's root

#### Scenario: Member creates a nested folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` creates a folder naming an existing folder on the same site as its parent
- **THEN** `pantheon-service` persists it as a child of that folder

#### Scenario: View-only member cannot create a folder
- **WHEN** a construction site member whose `DOCUMENT_PROJECTS` access is `VIEW` attempts to create a folder
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Folder and file audit metadata
`pantheon-service` SHALL record, for every folder, its creation date and creator and its last-updated date and last-updating member — renaming a folder or changing its linked task updates the latter pair. `pantheon-service` SHALL record, for every file, its upload date and uploading member. Responses SHALL resolve these ids to a display name (falling back to email when no display name is set), not raw identifiers.

#### Scenario: Folder shows who created and who last changed it
- **WHEN** a member requests a folder's detail
- **THEN** `pantheon-service` returns its creation date and creator's display name, and its last-updated date and last-updating member's display name

#### Scenario: Renaming a folder updates its audit fields
- **WHEN** a member with `MANAGE` access renames a folder
- **THEN** `pantheon-service` updates that folder's last-updated date and last-updating member to the acting member

### Requirement: Folder rename
`pantheon-service` SHALL allow a member with `MANAGE` access to `DOCUMENT_PROJECTS` to rename an existing folder.

#### Scenario: Member renames a folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` renames a folder
- **THEN** `pantheon-service` persists the new name and updates its audit metadata

### Requirement: Folder deletion
`pantheon-service` SHALL allow a member with `MANAGE` access to `DOCUMENT_PROJECTS` to delete a folder, permanently removing every folder and file nested under it, transitively, along with their object storage content. Deleting a folder SHALL NOT be blocked by any file or descendant folder being linked to a task.

#### Scenario: Deleting a folder removes its whole subtree
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` deletes a folder that contains nested folders and files
- **THEN** `pantheon-service` removes that folder, every folder and file nested under it, and their stored content, and none of them appear in any later listing

#### Scenario: Deleting an empty folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` deletes a folder with no contents
- **THEN** `pantheon-service` removes it

### Requirement: Folder task link, set only from Projetos
`pantheon-service` SHALL allow a member with `MANAGE` access to `DOCUMENT_PROJECTS` to link a folder to one `TaskCard` on the same construction site, or clear that link, from within the Projetos screen. Deleting the linked `TaskCard` SHALL clear the folder's link but SHALL NOT delete the folder.

#### Scenario: Member links a folder to a task
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` sets a folder's task link to an existing task card on the same site
- **THEN** `pantheon-service` persists the link and includes the task's title when the folder is later fetched

#### Scenario: Member clears a folder's task link
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` clears a folder's task link
- **THEN** `pantheon-service` removes the link without affecting the folder or the task

#### Scenario: Deleting the linked task does not delete the folder
- **WHEN** a `TaskCard` linked to a folder is deleted
- **THEN** `pantheon-service` clears that folder's task link and the folder, with its contents, remains unchanged

### Requirement: File upload
`pantheon-service` SHALL allow a member with `MANAGE` access to `DOCUMENT_PROJECTS` to upload a non-empty file directly into Projetos, either at the construction site's root or inside an existing folder on that site, stored in object storage under a key scoped to that site. Only files whose original filename extension (case-insensitive) is `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4` SHALL be accepted; any other extension SHALL be rejected. A `.mp4` file SHALL additionally be rejected if its duration exceeds 60 seconds, or if its duration cannot be determined.

#### Scenario: File uploaded into a folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` uploads a file naming an existing folder as its destination
- **THEN** `pantheon-service` stores the file and records a `SiteDocumentProjectAttachment` nested under that folder

#### Scenario: File uploaded at the site root
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` uploads a file naming no destination folder
- **THEN** `pantheon-service` stores the file at the construction site's root

#### Scenario: Empty file rejected
- **WHEN** a member attempts to upload an empty file
- **THEN** `pantheon-service` rejects the upload

#### Scenario: View-only member cannot upload directly
- **WHEN** a construction site member whose `DOCUMENT_PROJECTS` access is `VIEW` attempts to upload a file directly into Projetos
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Disallowed file type rejected
- **WHEN** a member attempts to upload a file whose extension is not `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4`
- **THEN** `pantheon-service` rejects the upload

#### Scenario: Allowed file types accepted
- **WHEN** a member uploads a file with extension `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4`
- **THEN** `pantheon-service` accepts and stores it

#### Scenario: Video within the duration cap accepted
- **WHEN** a member uploads an `.mp4` file whose duration is 60 seconds or less
- **THEN** `pantheon-service` accepts and stores it

#### Scenario: Video exceeding the duration cap rejected
- **WHEN** a member uploads an `.mp4` file whose duration exceeds 60 seconds
- **THEN** `pantheon-service` rejects the upload

#### Scenario: Video with undeterminable duration rejected
- **WHEN** a member uploads an `.mp4` file whose duration `pantheon-service` cannot determine
- **THEN** `pantheon-service` rejects the upload

### Requirement: File download and deletion
`pantheon-service` SHALL allow any member with at least `VIEW` access to `DOCUMENT_PROJECTS` to download a file's content, served with its original stored content type, and SHALL allow a member with `MANAGE` access to delete a file, removing it and its stored content.

#### Scenario: Member downloads a file
- **WHEN** a member with at least `VIEW` access to `DOCUMENT_PROJECTS` requests a file's content
- **THEN** `pantheon-service` returns the file's bytes with its original content type

#### Scenario: Member deletes a file
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` deletes a file
- **THEN** `pantheon-service` removes it and its stored content, and it no longer appears in any listing

### Requirement: Browsing folder contents
`pantheon-service` SHALL allow any member with at least `VIEW` access to `DOCUMENT_PROJECTS` to list the immediate child folders and files of a given folder, or of the construction site's root, and to fetch the ordered chain of ancestor folders (from the site's root down to a given folder) for breadcrumb navigation.

#### Scenario: Member lists root contents
- **WHEN** a member requests the contents of a construction site's Projetos root
- **THEN** `pantheon-service` returns every root-level folder and file, but not their descendants

#### Scenario: Member lists a folder's contents
- **WHEN** a member requests a folder's contents
- **THEN** `pantheon-service` returns that folder's immediate child folders and files, but not deeper descendants

#### Scenario: Member fetches a folder's breadcrumb path
- **WHEN** a member requests a nested folder's ancestor path
- **THEN** `pantheon-service` returns the ordered chain of folders from the site's root down to that folder, inclusive

### Requirement: Projetos folder-explorer UI
`pantheon-web` SHALL provide, within a construction site's menu, a "Projetos" view rendered as a folder explorer: breadcrumb navigation, a combined listing of the current folder's child folders and files with each item's name, an icon reflecting whether it is a folder or a file, and its last-updated date and last-updating member's display name, a same-level name filter, and controls (visible only to members with `MANAGE` access) to create a folder, upload a file, rename a folder, delete a folder or file, and set or clear a folder's task link. Clicking a file SHALL open an in-app preview matching its type (image, PDF, or video) rather than downloading it; a separate, always-available icon SHALL download the file directly. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member navigates into a folder
- **WHEN** a member clicks a folder row in the Projetos view
- **THEN** `pantheon-web` shows that folder's contents and updates the breadcrumb bar to include it

#### Scenario: Member navigates back via breadcrumbs
- **WHEN** a member clicks an ancestor in the breadcrumb bar
- **THEN** `pantheon-web` shows that ancestor folder's contents

#### Scenario: View-only member does not see management controls
- **WHEN** a construction site member whose `DOCUMENT_PROJECTS` access is `VIEW` opens the Projetos view
- **THEN** `pantheon-web` shows the folder/file listing and download actions but not create, upload, rename, delete, or task-link controls

#### Scenario: Member filters the current folder's contents
- **WHEN** a member types into the name filter while a folder's contents are shown
- **THEN** `pantheon-web` shows only the items in that folder whose name matches, without navigating or requesting deeper levels

#### Scenario: Clicking an image, PDF, or video file previews it
- **WHEN** a member clicks a file whose type is an image, a PDF, or a video
- **THEN** `pantheon-web` opens an in-app preview showing that file's content, without triggering a file download

#### Scenario: Downloading remains available from the preview and the row
- **WHEN** a member wants to save a file to disk, from either the file's row or an open preview
- **THEN** `pantheon-web` provides a distinct download control that downloads the file, separate from the click that opens its preview
