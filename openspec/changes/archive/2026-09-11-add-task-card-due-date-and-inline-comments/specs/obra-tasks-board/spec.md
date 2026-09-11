## ADDED Requirements

### Requirement: Card due date
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to set or clear a `TaskCard`'s due date (an optional, day-granularity date with no associated time), both at creation and afterward; `pantheon-web` SHALL display a card's due date on its face on the board when set.

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
