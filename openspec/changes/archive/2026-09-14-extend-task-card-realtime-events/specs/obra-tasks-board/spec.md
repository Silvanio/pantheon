## MODIFIED Requirements

### Requirement: Tasks tab per obra
`pantheon-web` SHALL provide a "Tasks" tab within each construction site's menu, rendering a board of the company's shared columns populated with that obra's own cards. A site member who is not company staff (no `CompanyMembership`, only a `SiteMembership` on this obra) and therefore cannot read the company's predefined label catalog SHALL still see the board load successfully; only the predefined-label picker degrades (empty), not the whole board.

#### Scenario: Member opens the Tasks tab
- **WHEN** a construction site member opens the "Tasks" tab
- **THEN** `pantheon-web` shows the company's columns with that obra's cards placed inside them

#### Scenario: Site-only member's board load is not blocked by the label catalog
- **WHEN** a site-only member (no `CompanyMembership`) opens the Tasks tab or performs an action that reloads the board
- **THEN** `pantheon-web` loads and shows the board successfully, and does not report the action as failed, even though that member's request for the company's predefined label catalog is rejected

### Requirement: Card creation
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` on a construction site to create a card on that obra's board, placed into one of the company's columns. `pantheon-service` SHALL emit a real-time event, scoped to the card's company, describing the created card, so that other members of that company with the board open see the new card without reloading.

#### Scenario: Member creates a card
- **WHEN** a member with `MANAGE` access to `TASKS` creates a card with a title in a given column
- **THEN** `pantheon-service` persists the card on that obra's board in the chosen column

#### Scenario: Other viewers see the new card in real time
- **WHEN** a card is created on one obra's board
- **THEN** `pantheon-web` clients belonging to the same company with that obra's Tasks board open show the new card without a manual reload

### Requirement: Card deletion
`pantheon-service` SHALL allow a member with `MANAGE` access to `TASKS` to delete a card from that obra's board, removing its comments, its own custom labels, its label attachments, and its assignees along with it. `pantheon-service` SHALL emit a real-time event, scoped to the card's company, describing the deletion, so that other members of that company with the board open see the card disappear without reloading.

#### Scenario: Member deletes a card
- **WHEN** a member with `MANAGE` access to `TASKS` deletes a card
- **THEN** `pantheon-service` removes the card and its dependent comments, custom labels, label attachments, and assignees, and `pantheon-web` no longer shows it on the board

#### Scenario: Other viewers see the deletion in real time
- **WHEN** a card is deleted on one obra's board
- **THEN** `pantheon-web` clients belonging to the same company with that obra's Tasks board open remove the card without a manual reload, closing that card's detail modal if it was open

## ADDED Requirements

### Requirement: Real-time sync of in-place card edits
`pantheon-service` SHALL emit a real-time event, scoped to the card's company, whenever an existing card is edited in place — its due date, its assigned site members, its attached or created labels, or its comment count — so that other members of that company with the board or that card's detail modal open see the change without reloading. The event SHALL include enough information (including full data for any label the edit newly attaches) to render the change without a follow-up fetch.

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
