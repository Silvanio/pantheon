# obra-tasks-board Specification

## Purpose
Defines the per-obra Tasks board: a Trello-like board of cards placed into the columns shared by the obra's company, scoped to that construction site, with due dates, labels, comments, and assignees.

## Requirements
### Requirement: Tasks tab per obra
`pantheon-web` SHALL provide a "Tasks" tab within each construction site's menu, rendering a board of the company's shared columns populated with that obra's own cards. A site member who is not company staff (no `CompanyMembership`, only a `SiteMembership` on this obra) and therefore cannot read the company's predefined label catalog SHALL still see the board load successfully; only the predefined-label picker degrades (empty), not the whole board.

#### Scenario: Member opens the Tasks tab
- **WHEN** a construction site member opens the "Tasks" tab
- **THEN** `pantheon-web` shows the company's columns with that obra's cards placed inside them

#### Scenario: Site-only member's board load is not blocked by the label catalog
- **WHEN** a site-only member (no `CompanyMembership`) opens the Tasks tab or performs an action that reloads the board
- **THEN** `pantheon-web` loads and shows the board successfully, and does not report the action as failed, even though that member's request for the company's predefined label catalog is rejected

### Requirement: Cards scoped to one obra
`pantheon-service` SHALL scope every `TaskCard` to exactly one `constructionSiteId`; creating, listing, or moving cards on one obra SHALL never expose or modify another obra's cards, even when both obras share the same columns.

#### Scenario: Card created in one obra is invisible in another
- **WHEN** a card is created on one construction site's task board
- **THEN** requesting the task board of a different construction site under the same company does not include that card

### Requirement: Card creation
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to create a card on that obra's board, placed into one of the company's columns. `pantheon-service` SHALL emit a real-time event, scoped to the card's company, describing the created card, so that other members of that company with the board open see the new card without reloading.

#### Scenario: Member creates a card
- **WHEN** a member with `MANAGE` access to `TASKS` creates a card with a title in a given column
- **THEN** `pantheon-service` persists the card on that obra's board in the chosen column

#### Scenario: Other viewers see the new card in real time
- **WHEN** a card is created on one obra's board
- **THEN** `pantheon-web` clients belonging to the same company with that obra's Tasks board open show the new card without a manual reload

### Requirement: Card movement between columns
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to move any of that obra's cards to any of the company's columns. When a card is moved, `pantheon-service` SHALL emit a real-time event, scoped to the card's company, describing the move, so that other members of that company with the board open see the card move without reloading.

#### Scenario: Card moved to another column
- **WHEN** a member with `MANAGE` access to `TASKS` moves a card from one column to another
- **THEN** `pantheon-service` updates the card's column and returns the updated board

#### Scenario: Other viewers see the move in real time
- **WHEN** a card is moved on one obra's board
- **THEN** `pantheon-web` clients belonging to the same company with that obra's Tasks board open reflect the card's new column and position without a manual reload

#### Scenario: Members of a different company do not see the move
- **WHEN** a card is moved on one company's obra board
- **THEN** `pantheon-web` clients belonging only to a different company do not receive an event for that move

### Requirement: Comments on cards
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to add a comment to any card in that obra, and allow any member with at least `VIEW` access to `TASKS` to read a card's comments.

#### Scenario: Member adds a comment
- **WHEN** a member with `MANAGE` access to `TASKS` posts a comment on a card
- **THEN** `pantheon-service` persists the comment attributed to that member

#### Scenario: View-only member reads comments
- **WHEN** a member with `VIEW`-only access to `TASKS` requests a card's comments
- **THEN** `pantheon-service` returns the existing comments

### Requirement: Card due date
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to set or clear a `TaskCard`'s due date (an optional, day-granularity date with no associated time), both at creation and afterward; `pantheon-web` SHALL display a card's due date as a small, discreet marker in the top-right corner of the card face when set, rendered in a distinct color once the due date has been reached or passed.

#### Scenario: Member sets a due date at creation
- **WHEN** a member with `MANAGE` access to `TASKS` creates a card with a due date
- **THEN** `pantheon-service` persists the due date on the card and `pantheon-web` shows it in the card face's top-right corner

#### Scenario: Member edits an existing card's due date
- **WHEN** a member with `MANAGE` access to `TASKS` updates a card's due date
- **THEN** `pantheon-service` persists the new due date and `pantheon-web` reflects it on the card face

#### Scenario: Member clears a card's due date
- **WHEN** a member with `MANAGE` access to `TASKS` clears a card's due date
- **THEN** `pantheon-service` removes the due date and `pantheon-web` no longer shows a due date on that card

#### Scenario: Card has no due date
- **WHEN** a card has no due date set
- **THEN** `pantheon-web` shows the card without a due date indicator

#### Scenario: Due date reached or passed is visually flagged
- **WHEN** a card's due date is today or earlier
- **THEN** `pantheon-web` renders that card's due date in a distinct color from a card whose due date is still in the future

### Requirement: Labels visible on the card face
`pantheon-web` SHALL render each card's attached labels on the card face on the board as small, discreet pills showing each label's name and color, without requiring the card's detail modal to be opened, and without the pills visually competing with the card's title.

#### Scenario: Card with labels shown on the board
- **WHEN** a card has one or more labels attached
- **THEN** `pantheon-web` shows each attached label's name and color as a small pill directly on the card face

### Requirement: Predefined labels attachable to cards
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to attach or detach any of that site's company's predefined labels (see `company-task-labels`) to or from a card on that obra's board.

#### Scenario: Member attaches a predefined label to a card
- **WHEN** a member with `MANAGE` access to `TASKS` attaches one of the company's predefined labels to a card
- **THEN** `pantheon-service` shows that label on the card

#### Scenario: Member detaches a predefined label from a card
- **WHEN** a member with `MANAGE` access to `TASKS` detaches a predefined label from a card
- **THEN** `pantheon-service` no longer shows that label on the card

### Requirement: Card-only custom labels
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to create a custom label (name and color) directly on a card, scoped only to that card — never added to the company's predefined catalog and never offered on any other card — automatically attached to the card at the moment it is created.

#### Scenario: Member creates a custom label on a card
- **WHEN** a member with `MANAGE` access to `TASKS` creates a custom label on a card
- **THEN** `pantheon-service` persists it scoped only to that card and `pantheon-web` shows it already attached, with no separate attach step

#### Scenario: Custom label invisible on other cards
- **WHEN** a member opens a different card's label picker
- **THEN** `pantheon-service` does not offer another card's custom label as an option

### Requirement: Discreet comment count on the card face
`pantheon-web` SHALL show a small, discreet comment-count icon on each card face instead of listing or expanding comments inline; clicking anywhere on the card, including that icon, SHALL open the card's detail modal. Comments SHALL be readable and postable only from within that modal, never on a closed card.

#### Scenario: Card face shows a comment count icon
- **WHEN** a card has one or more comments
- **THEN** `pantheon-web` shows a small icon with the comment count on the card face, and shows neither comment bodies nor a comment form on the closed card

#### Scenario: Clicking the comment icon opens the modal
- **WHEN** a member clicks the comment-count icon on a card
- **THEN** `pantheon-web` opens that card's detail modal

#### Scenario: Comments only postable from the open modal
- **WHEN** a member wants to add a comment to a card
- **THEN** `pantheon-web` only accepts a new comment through that card's open detail modal

### Requirement: Card assignees
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to assign or unassign one or more of that obra's site members to a card; `pantheon-web` SHALL show a card's assignees as small avatar-initials on the card face.

#### Scenario: Member assigns a site member to a card
- **WHEN** a member with `MANAGE` access to `TASKS` assigns a site member to a card
- **THEN** `pantheon-service` persists the assignment and `pantheon-web` shows that member's initials on the card face

#### Scenario: Member unassigns a site member from a card
- **WHEN** a member with `MANAGE` access to `TASKS` unassigns a previously assigned site member from a card
- **THEN** `pantheon-service` removes the assignment and `pantheon-web` no longer shows that member's initials on the card face

#### Scenario: Card with multiple assignees
- **WHEN** a card has more than one assignee
- **THEN** `pantheon-web` shows avatar-initials for each assigned member on the card face

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

### Requirement: Real-time sync of in-place card edits
`pantheon-service` SHALL emit a real-time event, scoped to the card's company, whenever an existing card is edited in place — its due date, its assigned site members, its attached or created labels, its comment count, or its attachment count — so that other members of that company with the board or that card's detail modal open see the change without reloading. The event SHALL include enough information (including full data for any label the edit newly attaches) to render the change without a follow-up fetch.

#### Scenario: Due date change seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` sets or clears a card's due date
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's new due date without a manual reload

#### Scenario: Assignee change seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` assigns or unassigns a site member on a card
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's updated assignees without a manual reload

#### Scenario: Label change seen in real time, including a brand-new custom label
- **WHEN** a member with `MANAGE` access to `TASKS` attaches or detaches a predefined label, or creates a card-only custom label
- **THEN** `pantheon-web` clients belonging to the same company show the card's updated labels, correctly named and colored, without a manual reload and without needing to have independently fetched that label

#### Scenario: Comment count seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` posts a comment on a card
- **THEN** `pantheon-web` clients belonging to the same company see the card's comment count increase without a manual reload; the comment's own text is not pushed and still requires opening the card to read

#### Scenario: Attachment count seen in real time
- **WHEN** a file gains or loses a link to a card — attached from the card's detail modal, deleted directly from Projetos, or removed via a folder's cascading delete — the card's attachment count changes
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's updated attachment count without a manual reload

### Requirement: Attachment indicator on the card face
`pantheon-web` SHALL show a small, discreet icon on a card's face, next to the comment-count icon, whenever that card has at least one attached file; the icon SHALL NOT be shown when the card has no attachments. Attaching a file from the card's own detail modal SHALL make the icon appear immediately for the member who attached it, without waiting on a real-time round trip.

#### Scenario: Card face shows the attachment icon
- **WHEN** a card has one or more files attached to it
- **THEN** `pantheon-web` shows a small attachment icon on that card's face, next to the comment-count icon

#### Scenario: Card face has no attachment icon when there are no attachments
- **WHEN** a card has no files attached to it
- **THEN** `pantheon-web` does not show an attachment icon on that card's face

#### Scenario: Icon appears immediately after attaching from the modal
- **WHEN** a member with `MANAGE` access to `TASKS` attaches a file to a card from its detail modal
- **THEN** `pantheon-web` shows the attachment icon on that card's face without requiring a manual reload

### Requirement: Card detail modal renders opaquely
`pantheon-web` SHALL render the card detail modal on a fully opaque surface, never allowing the board behind it to show through.

#### Scenario: Modal opened over a populated board
- **WHEN** a member opens a card's detail modal
- **THEN** `pantheon-web` renders the modal's surface fully opaque, with no board content visible through it
