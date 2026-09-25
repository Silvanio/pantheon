## MODIFIED Requirements

### Requirement: Daily report views
`pantheon-web` SHALL provide a view for creating and editing a draft daily report with all of its sections, a submit action, a per-site report history/list view with a page-size selector (1, 5, or 10 results per page), and a read-only detail view for submitted reports whose page header shows a three-level breadcrumb (a back-arrow icon linking back to the report history, the obra's name linking to the obra, "Lista de Diário de Obra" linking back to the report history, and a final non-clickable "Diário de Obra" segment marking the current page) in place of a plain back button, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member fills and submits a report from the UI
- **WHEN** a project member fills in the daily report form sections and clicks submit
- **THEN** `pantheon-web` saves each section to `pantheon-service` and, on submit, transitions the report to `SUBMITTED` and shows it as read-only

#### Scenario: Member browses report history
- **WHEN** a project member opens a construction site's report history view
- **THEN** `pantheon-web` lists the site's daily reports ordered by date, each opening its detail view on selection

#### Scenario: Member changes the report list's page size
- **WHEN** a project member selects a different page-size option (1, 5, or 10) on the report history view
- **THEN** `pantheon-web` reloads the list from the first page using the newly selected size

#### Scenario: Breadcrumb returns to the report history, not the obra summary
- **WHEN** a project member opens a daily report from the report history view and then clicks the breadcrumb's "Lista de Diário de Obra" link
- **THEN** `pantheon-web` returns to the obra's Diário de Obra tab, not its Resumo tab
