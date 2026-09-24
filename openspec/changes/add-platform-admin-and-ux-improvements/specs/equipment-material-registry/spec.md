## MODIFIED Requirements

### Requirement: Equipment views
`pantheon-web` SHALL provide, on its own construction-site tab independent of any material or budget feature, views for registering and listing equipment (with a page-size selector offering 1, 5, or 10 results per page) and for updating an equipment's status, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: User manages equipment from the UI
- **WHEN** a user with permission to manage equipment submits the equipment registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new equipment in the site's equipment list, on the "Equipamentos" tab

#### Scenario: User changes the equipment list's page size
- **WHEN** a user selects a different page-size option (1, 5, or 10) on the equipment list
- **THEN** `pantheon-web` reloads the list from the first page using the newly selected size
