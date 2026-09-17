## MODIFIED Requirements

### Requirement: Card deletion
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to delete a card from that obra's board, removing its comments, its own custom labels, its label attachments, and its assignees along with it. Any Projetos folder or file linked to that card (see `site-document-projects`) SHALL have that link cleared, never be deleted. `pantheon-service` SHALL emit a real-time event, scoped to the card's company, describing the deletion, so that other members of that company with the board open see the card disappear without reloading.

#### Scenario: Member deletes a card
- **WHEN** a member with `MANAGE` access to `TASKS` deletes a card
- **THEN** `pantheon-service` removes the card and its dependent comments, custom labels, label attachments, and assignees, and `pantheon-web` no longer shows it on the board

#### Scenario: Deleting a card unlinks, but does not delete, its attached files or linked folder
- **WHEN** a member with `MANAGE` access to `TASKS` deletes a card that has files attached to it or a Projetos folder linked to it
- **THEN** `pantheon-service` clears the task reference on each of those files and on that folder, while the files, the folder, and its contents remain unchanged

#### Scenario: Other viewers see the deletion in real time
- **WHEN** a card is deleted on one obra's board
- **THEN** `pantheon-web` clients belonging to the same company with that obra's Tasks board open remove the card without a manual reload, closing that card's detail modal if it was open

## ADDED Requirements

### Requirement: Attaching a file to a card
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a card's construction site, who also has at least `VIEW` access to that site's `DOCUMENT_PROJECTS`, to attach a file to that card by uploading it and choosing a destination — an existing Projetos folder on the same site, or the site's root. The uploaded file SHALL be stored as a Projetos file (see `site-document-projects`) under the chosen destination, tagged with that card's id. This is the only way a file gains a link to a task; a file already stored in Projetos SHALL NOT be linkable to a task from the Projetos screen itself.

#### Scenario: Member attaches a file to a card into an existing folder
- **WHEN** a member with `MANAGE` access to `TASKS` uploads a file to a card, choosing an existing Projetos folder as the destination
- **THEN** `pantheon-service` stores the file under that folder, tags it with the card's id, and it appears in that folder's contents in Projetos

#### Scenario: Member attaches a file to a card at the Projetos root
- **WHEN** a member with `MANAGE` access to `TASKS` uploads a file to a card, choosing no destination folder
- **THEN** `pantheon-service` stores the file at the construction site's Projetos root, tagged with the card's id

#### Scenario: Member whose Projetos access is hidden cannot attach files
- **WHEN** a construction site member whose `DOCUMENT_PROJECTS` access is `HIDDEN` attempts to attach a file to a card
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without task-management access cannot attach files
- **WHEN** a construction site member whose `TASKS` access is `VIEW` attempts to attach a file to a card
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Viewing a card's attached files
`pantheon-service` SHALL allow any member with at least `VIEW` access to `TASKS` to list the files currently attached to a card, including each file's Projetos folder path, and to download one.

#### Scenario: Member views a card's attachments
- **WHEN** a member with at least `VIEW` access to `TASKS` opens a card's detail
- **THEN** `pantheon-web` shows every file attached to that card along with its Projetos folder path

#### Scenario: Member downloads a card's attached file
- **WHEN** a member with at least `VIEW` access to `TASKS` downloads a file listed on a card
- **THEN** `pantheon-service` returns that file's content

### Requirement: Card attachments section in the detail modal
`pantheon-web`'s card detail modal SHALL include an "Anexos" section showing the card's currently attached files (name and folder path, each downloadable) and, for members with `MANAGE` access to `TASKS` and at least `VIEW` access to `DOCUMENT_PROJECTS`, an upload control that lets the member pick a destination folder from the site's Projetos tree before uploading.

#### Scenario: Member attaches a file from the card detail modal
- **WHEN** a member with `MANAGE` access to `TASKS` opens a card's detail modal, picks a destination folder, and uploads a file
- **THEN** `pantheon-web` shows the new file in that card's "Anexos" section without a full page reload

#### Scenario: View-only member sees attachments but not the upload control
- **WHEN** a member with `VIEW`-only access to `TASKS` opens a card's detail modal
- **THEN** `pantheon-web` shows the card's "Anexos" section with existing files downloadable, but no upload control
