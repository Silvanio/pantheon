## REMOVED Requirements

### Requirement: Labels scoped to one obra
**Reason**: Labels are no longer scoped to a single obra. They now split into company-scoped predefined labels (see the new `company-task-labels` capability) and card-only custom labels (see "Card-only custom labels" below).
**Migration**: Existing per-obra `TaskLabel` rows are cleared as part of the rescoping migration; an obra now draws attachable labels from its company's predefined catalog instead of a per-obra list.

### Requirement: Inline comment expansion on the card face
**Reason**: Replaced by a discreet comment-count icon on the card face; comments are now read and posted only from the card's detail modal, never on a closed card.
**Migration**: None needed — this was `pantheon-web`-only UI behavior with no persisted state to migrate.

## MODIFIED Requirements

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

## ADDED Requirements

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
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to delete a card from that obra's board, removing its comments, its own custom labels, its label attachments, and its assignees along with it.

#### Scenario: Member deletes a card
- **WHEN** a member with `MANAGE` access to `TASKS` deletes a card
- **THEN** `pantheon-service` removes the card and its dependent comments, custom labels, label attachments, and assignees, and `pantheon-web` no longer shows it on the board

### Requirement: Card detail modal renders opaquely
`pantheon-web` SHALL render the card detail modal on a fully opaque surface, never allowing the board behind it to show through.

#### Scenario: Modal opened over a populated board
- **WHEN** a member opens a card's detail modal
- **THEN** `pantheon-web` renders the modal's surface fully opaque, with no board content visible through it
