## ADDED Requirements

### Requirement: Company-scoped predefined label catalog
`pantheon-service` SHALL maintain a company-scoped catalog of predefined Tasks-board labels (name and color), manageable only by a company member with role `ADMIN`, reused across every obra of that company.

#### Scenario: Admin creates a predefined label
- **WHEN** a company member with role `ADMIN` creates a predefined label with a name and color
- **THEN** `pantheon-service` persists it in that company's catalog, available to every obra of the company

#### Scenario: Admin removes a predefined label
- **WHEN** a company member with role `ADMIN` deletes a predefined label from the catalog
- **THEN** `pantheon-service` removes it from the catalog and detaches it from any card it was attached to

#### Scenario: Non-admin blocked from managing the catalog
- **WHEN** a company member with role `MEMBER` attempts to create, rename, or delete a predefined label
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: "Urgente" seeded for every company
`pantheon-service` SHALL ensure every company has a predefined "Urgente" label available from the start, without requiring the admin to create it manually.

#### Scenario: New company gets Urgente automatically
- **WHEN** a company is created
- **THEN** `pantheon-service` seeds a predefined "Urgente" label for that company as part of creation

### Requirement: Predefined labels configured alongside task columns
`pantheon-web` SHALL let the company admin manage the predefined label catalog from Company Settings, in the same place task columns are configured.

#### Scenario: Admin manages labels in Company Settings
- **WHEN** a company admin opens Company Settings
- **THEN** `pantheon-web` shows a predefined-labels management panel alongside the task-columns panel
