## MODIFIED Requirements

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
