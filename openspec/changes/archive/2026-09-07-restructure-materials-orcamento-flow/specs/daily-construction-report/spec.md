## MODIFIED Requirements

### Requirement: Materials received logging
`pantheon-service` SHALL allow one or more `DailyReportMaterialReceived` records, each a free-text material name with an optional unit of measure, to be recorded on a draft daily report.

#### Scenario: Material received entry added
- **WHEN** a project member submits a material name, optional unit, and a quantity for a draft daily report
- **THEN** `pantheon-service` adds a materials-received entry to that report
