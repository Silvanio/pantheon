## MODIFIED Requirements

### Requirement: Daily report creation
`pantheon-service` SHALL allow a member of a construction site (company staff or an active `SiteMembership`) with `MANAGE` access to that site's daily reports to create one `DailyReport` per site per calendar date, starting in `DRAFT` status.

#### Scenario: Member creates a daily report
- **WHEN** a construction site member with daily-report management access submits a report date for a site that has no existing report for that date
- **THEN** `pantheon-service` persists a new `DailyReport` in `DRAFT` status, assigned the next sequential number for that site

#### Scenario: Duplicate report for the same date rejected
- **WHEN** a construction site member attempts to create a daily report for a site and date that already has one
- **THEN** `pantheon-service` rejects the request and does not create a second report

#### Scenario: View-only member cannot create
- **WHEN** a construction site member whose daily-report access is `VIEW` attempts to create a daily report
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Daily report listing and detail
`pantheon-service` SHALL allow any member of a construction site to list its daily reports and view the full detail of one, including all of its sections, regardless of whether their daily-report access is `VIEW` or `MANAGE`.

#### Scenario: Member lists daily reports
- **WHEN** an authenticated member of a construction site requests the list of its daily reports
- **THEN** `pantheon-service` returns every `DailyReport` for that site, ordered by date

#### Scenario: Member views a daily report's detail
- **WHEN** an authenticated member of a construction site requests a specific daily report by id
- **THEN** `pantheon-service` returns the report with all of its sections (weather, hours, workforce, equipment usage, activities, occurrences, materials received, comments)
