# obra-tasks-board Specification

## Purpose
Defines the per-obra Tasks board: a Trello-like board of cards placed into the columns shared by the obra's company, scoped to that construction site, with due dates, labels, and comments.

## Requirements
### Requirement: Tasks tab per obra
`pantheon-web` SHALL provide a "Tasks" tab within each construction site's menu, rendering a board of the company's shared columns populated with that obra's own cards.

#### Scenario: Member opens the Tasks tab
- **WHEN** a construction site member opens the "Tasks" tab
- **THEN** `pantheon-web` shows the company's columns with that obra's cards placed inside them

### Requirement: Cards scoped to one obra
`pantheon-service` SHALL scope every `TaskCard` to exactly one `constructionSiteId`; creating, listing, or moving cards on one obra SHALL never expose or modify another obra's cards, even when both obras share the same columns.

#### Scenario: Card created in one obra is invisible in another
- **WHEN** a card is created on one construction site's task board
- **THEN** requesting the task board of a different construction site under the same company does not include that card

### Requirement: Card creation
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to create a card on that obra's board, placed into one of the company's columns.

#### Scenario: Member creates a card
- **WHEN** a member with `MANAGE` access to `TASKS` creates a card with a title in a given column
- **THEN** `pantheon-service` persists the card on that obra's board in the chosen column

### Requirement: Card movement between columns
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to move any of that obra's cards to any of the company's columns.

#### Scenario: Card moved to another column
- **WHEN** a member with `MANAGE` access to `TASKS` moves a card from one column to another
- **THEN** `pantheon-service` updates the card's column and returns the updated board

### Requirement: Labels scoped to one obra
`pantheon-service` SHALL scope `TaskLabel`s to one `constructionSiteId`; a member with `MANAGE` access to `TASKS` SHALL be able to create a label and attach or detach it from that obra's cards.

#### Scenario: Label created and attached to a card
- **WHEN** a member with `MANAGE` access to `TASKS` creates a label and attaches it to a card in the same obra
- **THEN** `pantheon-service` shows that label on the card

### Requirement: Comments on cards
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to add a comment to any card in that obra, and allow any member with at least `VIEW` access to `TASKS` to read a card's comments.

#### Scenario: Member adds a comment
- **WHEN** a member with `MANAGE` access to `TASKS` posts a comment on a card
- **THEN** `pantheon-service` persists the comment attributed to that member

#### Scenario: View-only member reads comments
- **WHEN** a member with `VIEW`-only access to `TASKS` requests a card's comments
- **THEN** `pantheon-service` returns the existing comments

### Requirement: Card due date
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to set or clear a `TaskCard`'s due date (an optional, day-granularity date with no associated time), both at creation and afterward; `pantheon-web` SHALL display a card's due date on its face on the board when set, rendered in a distinct color once the due date has been reached or passed.

#### Scenario: Member sets a due date at creation
- **WHEN** a member with `MANAGE` access to `TASKS` creates a card with a due date
- **THEN** `pantheon-service` persists the due date on the card and `pantheon-web` shows it on the card face

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
`pantheon-web` SHALL render each card's attached labels on the card face on the board, showing each label's name and color, without requiring the card's detail modal to be opened.

#### Scenario: Card with labels shown on the board
- **WHEN** a card has one or more labels attached
- **THEN** `pantheon-web` shows each attached label's name and color directly on the card face

### Requirement: Inline comment expansion on the card face
`pantheon-web` SHALL provide a collapsed-by-default expandable section at the bottom of each card face; expanding it SHALL show that card's comments and a form to post a new one, without opening the card's detail modal. `pantheon-service` SHALL continue to enforce that only a member with at least `VIEW` access to `TASKS` can read a card's comments and only a member with `MANAGE` access to `TASKS` can post one, per the existing comments requirement.

#### Scenario: Member expands a card to read comments
- **WHEN** a member clicks a card's collapsed comment section
- **THEN** `pantheon-web` expands it in place and shows that card's existing comments, fetched without navigating away from the board

#### Scenario: Member posts a comment from the expanded card
- **WHEN** a member with `MANAGE` access to `TASKS` submits a comment from a card's expanded section
- **THEN** `pantheon-service` persists the comment and `pantheon-web` shows it in that card's expanded section

#### Scenario: Member collapses the section back
- **WHEN** a member clicks an already-expanded card's comment section
- **THEN** `pantheon-web` collapses it, hiding the comments until expanded again
