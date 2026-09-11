# global-tasks-board Specification

## Purpose
Defines the company-admin-only board that aggregates every obra's Tasks cards into one read-only view, color-coded by obra.

## Requirements
### Requirement: Admin-only aggregated board
`pantheon-service` SHALL expose an aggregated view combining every construction site's `TaskCard`s for a company into a single board, accessible only to a company member with role `ADMIN`.

#### Scenario: Admin views the global board
- **WHEN** a company member with role `ADMIN` requests the company's global task board
- **THEN** `pantheon-service` returns the shared columns with every obra's cards placed inside them

#### Scenario: Non-admin blocked from the global board
- **WHEN** a company member with role `MEMBER` requests the company's global task board
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Per-obra color label
`pantheon-service` SHALL annotate every card in the aggregated board with a color derived deterministically from its `constructionSiteId`, such that the same obra always renders the same color across requests, without persisting a color value.

#### Scenario: Same obra renders the same color on repeated requests
- **WHEN** the global task board is requested twice
- **THEN** cards belonging to the same obra carry the same color both times

#### Scenario: Cards from different obras are visually distinguishable
- **WHEN** the global task board includes cards from more than one obra
- **THEN** each card is annotated with its obra's name and derived color

### Requirement: Labels and due date visible on the aggregated board
`pantheon-service` SHALL include each card's label ids and due date in the aggregated board response, alongside a catalog of the labels needed to render them; `pantheon-web` SHALL render each card's labels and due date on the aggregated board the same way they render on that card's per-obra board.

#### Scenario: Aggregated board shows a card's labels and due date
- **WHEN** a company member with role `ADMIN` requests the company's global task board and a card has labels or a due date
- **THEN** `pantheon-web` shows that card's labels and due date on the aggregated board

### Requirement: Dashboard shortcut for admins
`pantheon-web` SHALL show a shortcut to the global Tasks board on the dashboard only to users whose active company membership role is `ADMIN`.

#### Scenario: Admin sees the shortcut
- **WHEN** a user with an active company membership role of `ADMIN` opens the dashboard
- **THEN** `pantheon-web` shows a shortcut to the global Tasks board

#### Scenario: Non-admin does not see the shortcut
- **WHEN** a user with an active company membership role of `MEMBER` opens the dashboard
- **THEN** `pantheon-web` does not show the shortcut to the global Tasks board
