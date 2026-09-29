## MODIFIED Requirements

### Requirement: Equipment usage logging
`pantheon-service` SHALL allow one or more `DailyReportEquipmentUsage` records to be recorded on a draft daily report, referencing either the site's registered `Equipment` or a free-text `customName` when the equipment isn't registered, and SHALL allow removing an entry while the report is still a draft.

#### Scenario: Registered equipment usage entry added
- **WHEN** a project member submits a reference to one of the site's registered equipment (with an optional status note) for a draft daily report
- **THEN** `pantheon-service` adds an equipment-usage entry to that report

#### Scenario: Unregistered equipment usage entry added
- **WHEN** a project member submits a free-text equipment name (not a registered equipment id), with an optional status note, for a draft daily report
- **THEN** `pantheon-service` adds an equipment-usage entry recording that name

#### Scenario: Equipment usage entry removed
- **WHEN** a project member removes an equipment-usage entry from a draft daily report
- **THEN** `pantheon-service` deletes that entry

### Requirement: Workforce presence logging
`pantheon-service` SHALL allow one or more `DailyReportWorkforceEntry` records (role description and headcount, optionally linked to a registered project member) to be recorded on a draft daily report, and SHALL allow removing an entry while the report is still a draft.

#### Scenario: Workforce entry added
- **WHEN** a project member submits a role description and headcount (optionally linked to a registered member) for a draft daily report
- **THEN** `pantheon-service` adds a workforce entry to that report

#### Scenario: Workforce entry removed
- **WHEN** a project member removes a workforce entry from a draft daily report
- **THEN** `pantheon-service` deletes that entry

### Requirement: Activity tracking
`pantheon-service` SHALL allow one or more `DailyReportActivity` records (description, progress note, and status of `IN_PROGRESS` or `COMPLETED`) to be recorded on a draft daily report, and SHALL allow removing an entry while the report is still a draft.

#### Scenario: Activity entry added
- **WHEN** a project member submits a description, progress note, and status for an activity on a draft daily report
- **THEN** `pantheon-service` adds an activity entry to that report

#### Scenario: Activity entry removed
- **WHEN** a project member removes an activity entry from a draft daily report
- **THEN** `pantheon-service` deletes that entry

## ADDED Requirements

### Requirement: Materials delivered on this report's date
`pantheon-service` SHALL expose, for a given daily report, the `Material` delivery-tracking records for that construction site whose `deliveredAt` falls on the report's `reportDate`, each showing its current delivery/checked status and a reference to its source Pedido de Compra. This is a read-only, computed view — it does not duplicate or replace `DailyReportMaterialReceived`'s independent free-text entries, which remain available on the same report for materials not tracked through a Pedido de Compra.

#### Scenario: Materials delivered that day are shown
- **WHEN** one or more `Material` records for a site were marked delivered on the same calendar date as a daily report's `reportDate`
- **THEN** `pantheon-service` includes them, with their status and source Pedido de Compra reference, when that report is requested

#### Scenario: A material delivered on a different date is not shown
- **WHEN** a `Material` record for the site was delivered on a date other than the report's `reportDate`
- **THEN** `pantheon-service` does not include it in that report's delivered-materials view

#### Scenario: Marking a delivered material as checked from the daily report
- **WHEN** a project member marks a delivered material as checked from within a daily report's view
- **THEN** `pantheon-service` updates that same `Material` record's status, visible identically from the Materiais tab
