# company-plan-catalog Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Registered plan catalog
`pantheon-service` SHALL maintain a registered catalog of plans — Basic, Profissional, and Ilimitado — each with a code, a display name, and an active-construction-site limit (Basic: 2, Profissional: 10, Ilimitado: unlimited), persisted as data rather than hardcoded in application logic.

#### Scenario: Catalog seeded
- **WHEN** `pantheon-service` starts against a fresh database
- **THEN** the `plan` catalog contains exactly Basic (limit 2), Profissional (limit 10), and Ilimitado (no limit)

#### Scenario: Catalog listed
- **WHEN** an authenticated user requests the list of available plans
- **THEN** `pantheon-service` returns every registered plan with its code, name, and active-site limit

### Requirement: Plan selection
`pantheon-service` SHALL allow the administrator of a company with no plan selected to choose one of the registered plans, recording it on the company at no charge.

#### Scenario: Admin selects a plan
- **WHEN** the administrator of a company with no plan submits one of the registered plan codes
- **THEN** `pantheon-service` records that plan on the company

#### Scenario: Non-admin cannot select a plan
- **WHEN** a user who is not the administrator of a company attempts to select its plan
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Plan change
`pantheon-service` SHALL allow the administrator of a company to change its plan at any time, provided the company's current number of active construction sites does not exceed the new plan's active-site limit.

#### Scenario: Admin upgrades plan
- **WHEN** the administrator of a company on the Basic plan selects the Profissional or Ilimitado plan
- **THEN** `pantheon-service` updates the company's plan immediately

#### Scenario: Downgrade blocked by active site count
- **WHEN** the administrator of a company attempts to select a plan whose active-site limit is lower than the company's current number of active construction sites
- **THEN** `pantheon-service` rejects the request and does not change the company's plan

### Requirement: Active construction-site limit enforcement
`pantheon-service` SHALL reject the creation of a new construction site for a company that has no plan selected or whose active construction-site count has already reached its plan's limit.

#### Scenario: Site creation blocked with no plan
- **WHEN** the administrator of a company with no plan selected attempts to create a construction site
- **THEN** `pantheon-service` rejects the request, indicating that a plan must be selected first

#### Scenario: Site creation blocked at plan limit
- **WHEN** the administrator of a company whose active construction-site count equals its plan's limit attempts to create another construction site
- **THEN** `pantheon-service` rejects the request

#### Scenario: Ilimitado plan has no limit
- **WHEN** the administrator of a company on the Ilimitado plan creates a construction site regardless of how many it already has
- **THEN** `pantheon-service` creates the site

