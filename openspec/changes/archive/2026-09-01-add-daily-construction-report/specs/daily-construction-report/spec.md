## ADDED Requirements

### Requirement: Daily report creation
`pantheon-service` SHALL allow a member of a construction site's project to create one `DailyReport` per site per calendar date, starting in `DRAFT` status.

#### Scenario: Member creates a daily report
- **WHEN** a project member submits a report date for a construction site that has no existing report for that date
- **THEN** `pantheon-service` persists a new `DailyReport` in `DRAFT` status, assigned the next sequential number for that site

#### Scenario: Duplicate report for the same date rejected
- **WHEN** a project member attempts to create a daily report for a site and date that already has one
- **THEN** `pantheon-service` rejects the request and does not create a second report

### Requirement: Weather condition logging
`pantheon-service` SHALL allow the weather condition and whether it blocked planned tasks to be recorded on a draft daily report.

#### Scenario: Weather recorded
- **WHEN** a project member submits a weather condition and whether it blocked planned tasks for a draft daily report
- **THEN** `pantheon-service` saves the weather condition and blocked-tasks flag on that report

### Requirement: Work hours logging
`pantheon-service` SHALL allow the start and end work hours for the day to be recorded on a draft daily report.

#### Scenario: Work hours recorded
- **WHEN** a project member submits a start and end time for a draft daily report
- **THEN** `pantheon-service` saves the work hours on that report

### Requirement: Workforce presence logging
`pantheon-service` SHALL allow one or more `DailyReportWorkforceEntry` records (role description and headcount, optionally linked to a registered project member) to be recorded on a draft daily report.

#### Scenario: Workforce entry added
- **WHEN** a project member submits a role description and headcount (optionally linked to a registered member) for a draft daily report
- **THEN** `pantheon-service` adds a workforce entry to that report

#### Scenario: Workforce entry pre-fills from a registered member
- **WHEN** a project member submits a workforce entry linked to a registered `ProjectMembership` without overriding the role description
- **THEN** `pantheon-service` pre-fills the entry's role description from that membership's construction function/specialty

### Requirement: Equipment usage logging
`pantheon-service` SHALL allow one or more `DailyReportEquipmentUsage` records, referencing the site's registered `Equipment`, to be recorded on a draft daily report.

#### Scenario: Equipment usage entry added
- **WHEN** a project member submits a reference to one of the site's registered equipment (with an optional status note) for a draft daily report
- **THEN** `pantheon-service` adds an equipment-usage entry to that report

### Requirement: Activity tracking
`pantheon-service` SHALL allow one or more `DailyReportActivity` records (description, progress note, and status of `IN_PROGRESS` or `COMPLETED`) to be recorded on a draft daily report.

#### Scenario: Activity entry added
- **WHEN** a project member submits a description, progress note, and status for an activity on a draft daily report
- **THEN** `pantheon-service` adds an activity entry to that report

### Requirement: Occurrence logging
`pantheon-service` SHALL allow one or more `DailyReportOccurrence` records (free-text) to be recorded on a draft daily report.

#### Scenario: Occurrence entry added
- **WHEN** a project member submits a description of an occurrence for a draft daily report
- **THEN** `pantheon-service` adds an occurrence entry to that report

### Requirement: Materials received logging
`pantheon-service` SHALL allow one or more `DailyReportMaterialReceived` records, referencing the site's registered `Material` catalog, to be recorded on a draft daily report.

#### Scenario: Material received entry added
- **WHEN** a project member submits a reference to one of the site's registered materials and a quantity for a draft daily report
- **THEN** `pantheon-service` adds a materials-received entry to that report

### Requirement: Report comments
`pantheon-service` SHALL allow free-text comments to be recorded on a draft daily report.

#### Scenario: Comment recorded
- **WHEN** a project member submits comment text for a draft daily report
- **THEN** `pantheon-service` saves the comment on that report

### Requirement: Daily report submission
`pantheon-service` SHALL allow a project member to submit a draft daily report, transitioning it to `SUBMITTED` status, after which its sections can no longer be edited.

#### Scenario: Report submitted
- **WHEN** a project member submits a draft daily report for submission
- **THEN** `pantheon-service` transitions the report's status to `SUBMITTED`

#### Scenario: Submitted report rejects further edits
- **WHEN** a project member attempts to edit a section of a report that is in `SUBMITTED` status
- **THEN** `pantheon-service` rejects the request

### Requirement: Daily report listing and detail
`pantheon-service` SHALL allow any member of a project to list a construction site's daily reports and view the full detail of one, including all of its sections.

#### Scenario: Member lists daily reports
- **WHEN** an authenticated member of a project requests the list of daily reports for one of its construction sites
- **THEN** `pantheon-service` returns every `DailyReport` for that site, ordered by date

#### Scenario: Member views a daily report's detail
- **WHEN** an authenticated member of a project requests a specific daily report by id
- **THEN** `pantheon-service` returns the report with all of its sections (weather, hours, workforce, equipment usage, activities, occurrences, materials received, comments)

### Requirement: Daily report views
`pantheon-web` SHALL provide a view for creating and editing a draft daily report with all of its sections, a submit action, a per-site report history/list view, and a read-only detail view for submitted reports, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member fills and submits a report from the UI
- **WHEN** a project member fills in the daily report form sections and clicks submit
- **THEN** `pantheon-web` saves each section to `pantheon-service` and, on submit, transitions the report to `SUBMITTED` and shows it as read-only

#### Scenario: Member browses report history
- **WHEN** a project member opens a construction site's report history view
- **THEN** `pantheon-web` lists the site's daily reports ordered by date, each opening its detail view on selection
