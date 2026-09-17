## MODIFIED Requirements

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
