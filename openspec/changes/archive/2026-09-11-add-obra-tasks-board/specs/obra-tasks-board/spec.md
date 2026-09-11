## ADDED Requirements

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
