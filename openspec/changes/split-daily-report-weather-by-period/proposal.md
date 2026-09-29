## Why

Weather can change over the course of a work day and affect the morning and afternoon shifts differently. Today Diário de Obra only records one weather condition for the whole day; the user wants it recorded separately for Manhã (morning) and Tarde (afternoon).

## What Changes

- `DailyReport`'s single `weatherCondition` field is replaced by two: `weatherConditionMorning` and `weatherConditionAfternoon` (same 5-option set: `SUNNY`/`PARTLY_CLOUDY`/`CLOUDY`/`RAINY`/`STORM`). Both become required to save the report's core section, mirroring today's single-field requirement.
- `weatherBlockedTasks` stays a single day-level flag — not split by period (not requested, and there's no per-period task-blocking concept elsewhere in the report).
- Existing reports' single `weatherCondition` value is copied into both new columns during migration, so historical reports keep showing a (identical) value for both periods rather than a blank afternoon.
- `pantheon-web` and `pantheon-mobile` core-section forms/detail views show two weather pickers (Manhã / Tarde) instead of one.

## Capabilities

### Modified Capabilities
- `daily-construction-report`: the "Weather condition logging" requirement changes from a single day-level weather condition to two, one per period (Manhã/Tarde).

## Impact

- **Affected code**: `pantheon-service` — `DailyReport` entity, `DailyReportCoreUpdateRequest`/`DailyReportResponse`/`DailyReportDetailResponse` DTOs, `DailyReportService.updateCore`/core-fields-required validation, new Flyway migration. `pantheon-web` — the core-section form/display (weather picker × 2). `pantheon-mobile` — same, in the daily report detail screen's core-fields editor.
- **Data migration**: existing `weather_condition` value copied into both new columns; old column dropped.
