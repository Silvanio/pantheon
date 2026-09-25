# global-tasks-board Specification

## Purpose
Defines the company-admin-only board that aggregates every obra's Tasks cards into one read-only view, color-coded by obra.
## Requirements
### Requirement: Admin-only aggregated board
`pantheon-service` SHALL expose an aggregated view combining every construction site's `TaskCard`s for a company into a single board, accessible to a company member with role `ADMIN` or to a platform superadmin (see `platform-admin`), regardless of the superadmin's own company membership.

#### Scenario: Admin views the global board
- **WHEN** a company member with role `ADMIN` requests the company's global task board
- **THEN** `pantheon-service` returns the shared columns with every obra's cards placed inside them

#### Scenario: Non-admin blocked from the global board
- **WHEN** a company member with role `MEMBER` requests the company's global task board
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Superadmin views the global board without any company membership
- **WHEN** a platform superadmin with no `CompanyMembership` for a company requests that company's global task board
- **THEN** `pantheon-service` returns the shared columns with every obra's cards placed inside them, the same as for a company `ADMIN`

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

### Requirement: Filter the Global Tasks Board by obra
`pantheon-web` SHALL provide, on the Global Tasks Board, a filter control for narrowing the aggregated view down to a single obra. The control SHALL show at most 10 obras at a time; while the search field holds fewer than 3 characters it SHALL list the company's obras (up to that limit) unfiltered, and once 3 or more characters are entered it SHALL show only the obras whose name contains the typed text (case-insensitive), still capped at 10. Selecting an obra SHALL restrict every column's cards to that obra's cards only; a visible action SHALL clear the filter back to showing every obra's cards.

#### Scenario: Filter narrows the board to one obra
- **WHEN** a company admin (or superadmin) selects a specific obra from the filter
- **THEN** `pantheon-web` shows, in every column, only the cards belonging to that obra

#### Scenario: Typing fewer than 3 characters does not filter
- **WHEN** a user types 1 or 2 characters into the obra search field
- **THEN** `pantheon-web` continues showing the company's obras unfiltered (up to 10), not yet applying a name match

#### Scenario: Typing 3 or more characters filters by name
- **WHEN** a user types 3 or more characters that match part of some obras' names
- **THEN** `pantheon-web` shows only the matching obras, up to 10, case-insensitively

#### Scenario: Clearing the filter restores the full board
- **WHEN** a user clears an active obra filter
- **THEN** `pantheon-web` shows every obra's cards again in each column

### Requirement: Card summary detail on the Global Tasks Board
Each card on the Global Tasks Board SHALL show the same at-a-glance summary as its per-obra Tasks board counterpart: the obra name badge (unchanged), its labels, its due date (highlighted when due or overdue), up to 3 assignee initials (with a tooltip showing the full name, and a "+N" overflow badge beyond 3), an attachment indicator icon when it has at least one attachment (tooltip-only, no count shown, matching the per-obra board), and a comment-count icon with the number when it has at least one comment. `pantheon-service`'s aggregated board response SHALL include, per card, its assignee site-membership ids, comment count, and attachment count, plus a company-wide resolved list of every referenced assignee's display name (falling back to the accountless membership's own stored name when it has no linked `AppUser`, mirroring the per-obra member-listing resolution).

#### Scenario: Card shows its assignees, attachment indicator, and comment count
- **WHEN** a card on the Global Tasks Board has one or more assignees, at least one attachment, and at least one comment
- **THEN** `pantheon-web` shows the assignees' initials (up to 3, with a name tooltip), an attachment icon, and a comment icon with the comment count

#### Scenario: Assignee name resolves correctly for an accountless service-provider membership
- **WHEN** a card is assigned to a site membership with no linked `AppUser` (e.g. a service-provider member added without their own account)
- **THEN** `pantheon-web` shows that membership's own stored display name, not a blank or fallback value

#### Scenario: Indicators are omitted when there is nothing to show
- **WHEN** a card has no assignees, no attachments, and no comments
- **THEN** `pantheon-web` shows none of those three indicators, only the title, labels (if any), due date (if any), and obra badge

