## Context

`ConstructionSite` (from `add-construction-site-and-team`) and `Equipment`/`Material` (from `add-equipment-and-material-registry`) exist but have no day-to-day operating record. This change adds that record — the daily construction report — mirroring the reference competitor's "Recursos Principais" list almost one-to-one, since that list *is* the RDO's structure. As with the two prior changes, all identifiers (entities, tables, columns, enum values) are English; user-facing text is Portuguese, sourced from `pantheon-web`'s `pt-BR` locale resource file.

## Goals / Non-Goals

**Goals:**
- One `DailyReport` per site per calendar date, holding all eight logged sections.
- A simple draft → submitted lifecycle (no multi-stage approval in this change — approval/signatures are `add-daily-report-media-and-signoff`'s concern).
- Reuse `Equipment`/`Material` catalog entries by reference rather than free text, so usage data stays queryable.

**Non-Goals:**
- Media, attachments, signatures, PDF export — next change.
- Multiple reports per site per day, or backdating reports arbitrarily far (see Open Questions).
- Automatic/integrated weather data — condition is entered manually by whoever fills the report.
- Aggregated analytics/dashboards over report history (e.g., cumulative hours charts) — this change only stores and lists the data; analytics is future work.

## Decisions

### Entity model
```
DailyReport                              -- table: daily_report
  id                    UUID PK
  construction_site_id  UUID FK construction_site.id
  report_date           DATE
  sequence_no           INT           -- sequential per site, e.g. report #1, #2...
  status                VARCHAR       -- DRAFT | SUBMITTED
  weather_condition       VARCHAR NULL   -- e.g. "Sunny", "Rainy"
  weather_blocked_tasks   BOOLEAN NULL   -- did weather prevent planned tasks?
  work_hours_start        TIME NULL
  work_hours_end          TIME NULL
  comments                 TEXT NULL
  created_by    UUID FK app_user.id
  submitted_at  TIMESTAMPTZ NULL
  created_at / updated_at
  UNIQUE (construction_site_id, report_date)

DailyReportWorkforceEntry   -- table: daily_report_workforce_entry (0..N per DailyReport)
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  membership_id     UUID NULL FK project_membership.id   -- set when it's a registered member
  role_description  VARCHAR   -- e.g. "Bricklayer", "Laborer" (mirrors function/specialty, but free entry here too for casual labor)
  headcount         INT

DailyReportEquipmentUsage   -- table: daily_report_equipment_usage (0..N per DailyReport)
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  equipment_id      UUID FK equipment.id
  status_note       VARCHAR NULL   -- free note, e.g. "Operating normally", "Down for maintenance"

DailyReportActivity   -- table: daily_report_activity (0..N per DailyReport)
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  description       VARCHAR
  progress_note     VARCHAR    -- free text or %, e.g. "60%", "Done"
  status            VARCHAR    -- IN_PROGRESS | COMPLETED

DailyReportOccurrence   -- table: daily_report_occurrence (0..N per DailyReport)
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  description       TEXT

DailyReportMaterialReceived   -- table: daily_report_material_received (0..N per DailyReport)
  id                UUID PK
  daily_report_id   UUID FK daily_report.id
  material_id       UUID FK material.id
  quantity          NUMERIC
```
`DailyReport` itself holds the single-valued sections (weather, hours, comments); the repeatable sections (workforce, equipment usage, activities, occurrences, materials received) are child tables, each a simple list scoped to the report — this avoids cramming a variable number of rows into JSON blobs and keeps each section independently queryable (e.g., "total hours across a project" or "how often was a given Equipment record used").

*Alternative considered*: a single JSONB column per section — rejected because equipment/material usage need real foreign keys for referential integrity and future reporting, and workforce/activity/occurrence lists benefit from being real rows for the same reason.

### Draft/submit lifecycle
A report starts `DRAFT` and any project member of that site can edit any section while in that state (last write wins — no field-level locking). `POST /api/daily-reports/{id}/submit` transitions it to `SUBMITTED`, after which section edits are rejected by the API. This mirrors "Assinaturas: define responsáveis por validar e aprovar" from the competitor list without implementing signatures yet — submission is the placeholder gate that the next change's signature step will attach to.

*Alternative considered*: no status at all, reports always editable — rejected because the competitor's own list treats "assinatura"/approval as a real step, implying a report becomes authoritative at some point; having `DRAFT`/`SUBMITTED` now avoids a breaking status-field migration later.

### One report per site per date
Enforced via a DB unique constraint on `(construction_site_id, report_date)`. If a second entry for the same date is needed (e.g., correcting an error), this change does not support it — see Open Questions.

## Risks / Trade-offs

- **[Risk]** "Last write wins" on a shared draft report could silently drop one person's edits if two people edit `DailyReport` sections concurrently → **Mitigation**: acceptable for this phase (small crews, sequential real-world editing); optimistic locking can be added later without an API shape change.
- **[Risk]** `DailyReportWorkforceEntry.role_description` duplicates information already available via `membership_id` → `ProjectMembership.function`/`specialty` when set, risking drift between the two → **Mitigation**: when `membership_id` is set, the API pre-fills `role_description` from the membership's function/specialty but still allows override, since a registered service provider's day-to-day role can vary by report.
- **[Risk]** No backdating guard means reports could be created for dates far in the past or future → **Mitigation**: not restricted in this change; flagged as an Open Question.

## Migration Plan

Additive only: new `daily_report`, `daily_report_workforce_entry`, `daily_report_equipment_usage`, `daily_report_activity`, `daily_report_occurrence`, `daily_report_material_received` tables via new Flyway migrations. No existing table/endpoint changes. Rollback: drop the six new tables.

## Open Questions

- Should report dates be restricted to "today or the recent past" (e.g., no more than N days back, no future dates), or fully unrestricted? This design leaves it unrestricted; revisit if backdating abuse becomes a real concern.
- If a report is submitted (`SUBMITTED`) in error, is there a way to reopen it to `DRAFT`? This design has no reopen endpoint; a submitted report is final until the signature/approval change (next) defines a correction flow.
- `sequence_no` — is it purely cosmetic (display "report #12") or does anything depend on strict contiguity? This design treats it as cosmetic, computed as `count(existing reports for this site) + 1` at creation time, with no gap-filling guarantee if a report is ever deleted (deletion is not supported by this change, so gaps shouldn't occur in practice).
