## MODIFIED Requirements

### Requirement: Daily report submission
`pantheon-service` SHALL allow a construction site member with `MANAGE` access to that site's daily reports to submit a draft daily report for approval, transitioning it to `PENDING_APPROVAL` status and creating its ordered approval steps (see `daily-report-approval-workflow`), after which its sections can no longer be edited unless a step is later rejected (returning it to `DRAFT`).

#### Scenario: Report submitted for approval
- **WHEN** a construction site member with `MANAGE` access submits a draft daily report
- **THEN** `pantheon-service` transitions the report's status to `PENDING_APPROVAL` and creates its approval steps for a new cycle

#### Scenario: Pending-approval report rejects further edits
- **WHEN** a project member attempts to edit a section of a report that is in `PENDING_APPROVAL` status
- **THEN** `pantheon-service` rejects the request

### Requirement: Daily report listing and detail
`pantheon-service` SHALL allow a member of a construction site whose resolved `DAILY_REPORT` access is `MANAGE` or `VIEW_AND_APPROVE` to list its daily reports — paginated, sorted by creation date descending (newest first) — and view the full detail of any one, in any status. A member whose resolved `DAILY_REPORT` access is `VIEW` SHALL only see (in listings and on direct access) reports whose status is `APPROVED`; a `DRAFT` or `PENDING_APPROVAL` report SHALL behave as if it does not exist to that member.

#### Scenario: Manage or approver member lists all reports
- **WHEN** an authenticated member with `MANAGE` or `VIEW_AND_APPROVE` `DAILY_REPORT` access requests a page of a construction site's daily reports
- **THEN** `pantheon-service` returns that page of `DailyReport`s in any status for that site, ordered by creation date descending, along with the total element and page counts

#### Scenario: Newest report appears first
- **WHEN** a construction site has multiple daily reports and a `MANAGE` or `VIEW_AND_APPROVE` member requests the first page
- **THEN** the most recently created `DailyReport` is the first item returned

#### Scenario: Manage or approver member views any report's detail
- **WHEN** an authenticated member with `MANAGE` or `VIEW_AND_APPROVE` `DAILY_REPORT` access requests a specific daily report by id, in any status
- **THEN** `pantheon-service` returns the report with all of its sections (weather, hours, workforce, equipment usage, activities, occurrences, materials received, comments)

#### Scenario: View-only member only sees approved reports in listings
- **WHEN** an authenticated member whose resolved `DAILY_REPORT` access is `VIEW` requests a page of a construction site's daily reports
- **THEN** `pantheon-service` returns only the reports whose status is `APPROVED`

#### Scenario: View-only member cannot access a non-approved report directly
- **WHEN** an authenticated member whose resolved `DAILY_REPORT` access is `VIEW` requests a specific daily report by id that is `DRAFT` or `PENDING_APPROVAL`
- **THEN** `pantheon-service` rejects the request as if the report does not exist

### Requirement: Daily report views
`pantheon-web` SHALL provide a view for creating and editing a draft daily report with all of its sections, a submit-for-approval action, a per-site report history/list view with a page-size selector (1, 5, or 10 results per page), and a read-only detail view for reports pending approval or approved, with approval actions shown to members whose resolved `DAILY_REPORT` access is `VIEW_AND_APPROVE` or `MANAGE`, whose page header shows a three-level breadcrumb (a back-arrow icon linking back to the report history, the obra's name linking to the obra, "Lista de Diário de Obra" linking back to the report history, and a final non-clickable "Diário de Obra" segment marking the current page) in place of a plain back button, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member fills and submits a report from the UI
- **WHEN** a project member fills in the daily report form sections and clicks the submit-for-approval action
- **THEN** `pantheon-web` saves each section to `pantheon-service` and, on submit, transitions the report to `PENDING_APPROVAL` and shows it as read-only with its approval steps

#### Scenario: Member browses report history
- **WHEN** a project member opens a construction site's report history view
- **THEN** `pantheon-web` lists the site's daily reports ordered by date (respecting that member's view-only vs. manage/approver visibility), each opening its detail view on selection
