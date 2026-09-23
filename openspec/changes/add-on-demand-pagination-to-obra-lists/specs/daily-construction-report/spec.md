## MODIFIED Requirements

### Requirement: Daily report listing and detail
`pantheon-service` SHALL allow any member of a construction site to list its daily reports — paginated, sorted by creation date descending (newest first) — and view the full detail of one, including all of its sections, regardless of whether their daily-report access is `VIEW` or `MANAGE`.

#### Scenario: Member lists daily reports
- **WHEN** an authenticated member of a construction site requests a page of its daily reports
- **THEN** `pantheon-service` returns that page of `DailyReport`s for that site, ordered by creation date descending, along with the total element and page counts

#### Scenario: Newest report appears first
- **WHEN** a construction site has multiple daily reports and a member requests the first page
- **THEN** the most recently created `DailyReport` is the first item returned

#### Scenario: Member views a daily report's detail
- **WHEN** an authenticated member of a construction site requests a specific daily report by id
- **THEN** `pantheon-service` returns the report with all of its sections (weather, hours, workforce, equipment usage, activities, occurrences, materials received, comments)

### Requirement: Daily report views
`pantheon-web` SHALL provide a view for creating and editing a draft daily report with all of its sections, a submit action, a paginated per-site report history/list view (fetching one page on demand, not the full history), and a read-only detail view for submitted reports, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member fills and submits a report from the UI
- **WHEN** a project member fills in the daily report form sections and clicks submit
- **THEN** `pantheon-web` saves each section to `pantheon-service` and, on submit, transitions the report to `SUBMITTED` and shows it as read-only

#### Scenario: Member browses report history
- **WHEN** a project member opens a construction site's report history view
- **THEN** `pantheon-web` lists one page of the site's daily reports ordered by creation date descending, each opening its detail view on selection, with prev/next controls to fetch adjacent pages on demand
