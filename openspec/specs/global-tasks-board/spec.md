# global-tasks-board Specification

## Purpose
TBD - created by syncing change add-obra-tasks-board. Update Purpose after archive.

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

### Requirement: Dashboard shortcut for admins
`pantheon-web` SHALL show a shortcut to the global Tasks board on the dashboard only to users whose active company membership role is `ADMIN`.

#### Scenario: Admin sees the shortcut
- **WHEN** a user with an active company membership role of `ADMIN` opens the dashboard
- **THEN** `pantheon-web` shows a shortcut to the global Tasks board

#### Scenario: Non-admin does not see the shortcut
- **WHEN** a user with an active company membership role of `MEMBER` opens the dashboard
- **THEN** `pantheon-web` does not show the shortcut to the global Tasks board
