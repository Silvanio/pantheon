## MODIFIED Requirements

### Requirement: List equipment of a construction site
`pantheon-service` SHALL allow any member of a construction site to list the `Equipment` registered for it — paginated, sorted by creation date descending (newest first).

#### Scenario: Member lists equipment
- **WHEN** an authenticated member of a construction site requests a page of its equipment
- **THEN** `pantheon-service` returns that page of `Equipment` records linked to that site, including each one's current status, ordered by creation date descending, along with the total element and page counts

#### Scenario: Newest equipment appears first
- **WHEN** a construction site has multiple equipment records and a member requests the first page
- **THEN** the most recently created `Equipment` record is the first item returned

### Requirement: Equipment views
`pantheon-web` SHALL provide, on its own construction-site tab independent of any material or budget feature, a paginated view (fetching one page on demand, not the full list) for listing equipment, a view for registering equipment, and a view for updating an equipment's status, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: User manages equipment from the UI
- **WHEN** a user with permission to manage equipment submits the equipment registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new equipment in the site's equipment list, on the "Equipamentos" tab

#### Scenario: User pages through the equipment list
- **WHEN** a user on the "Equipamentos" tab has more equipment than fits on one page
- **THEN** `pantheon-web` shows prev/next controls that fetch the adjacent page on demand
