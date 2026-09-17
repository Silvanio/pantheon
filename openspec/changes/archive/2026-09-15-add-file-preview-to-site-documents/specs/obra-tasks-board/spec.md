## MODIFIED Requirements

### Requirement: Card attachments section in the detail modal
`pantheon-web`'s card detail modal SHALL include an "Anexos" section showing the card's currently attached files (name and folder path). Clicking an attachment SHALL open an in-app preview matching its type (image, PDF, or video) rather than downloading it; a separate, always-available icon SHALL download the file directly. For members with `MANAGE` access to `TASKS` and at least `VIEW` access to `DOCUMENT_PROJECTS`, the section SHALL also offer an upload control that lets the member pick a destination folder from the site's Projetos tree before uploading.

#### Scenario: Member attaches a file from the card detail modal
- **WHEN** a member with `MANAGE` access to `TASKS` opens a card's detail modal, picks a destination folder, and uploads a file
- **THEN** `pantheon-web` shows the new file in that card's "Anexos" section without a full page reload

#### Scenario: View-only member sees attachments but not the upload control
- **WHEN** a member with `VIEW`-only access to `TASKS` opens a card's detail modal
- **THEN** `pantheon-web` shows the card's "Anexos" section with existing files downloadable, but no upload control

#### Scenario: Clicking an attachment previews it
- **WHEN** a member clicks an attachment listed in a card's "Anexos" section
- **THEN** `pantheon-web` opens an in-app preview showing that file's content, without triggering a file download

#### Scenario: Downloading an attachment remains a separate action
- **WHEN** a member wants to save an attachment to disk
- **THEN** `pantheon-web` provides a distinct download control in the "Anexos" list, separate from the click that opens its preview
