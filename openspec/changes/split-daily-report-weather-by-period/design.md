## Context

`DailyReport.weatherCondition` (a `String`, one of 5 fixed values) is a single day-level field, required alongside `workHoursStart`/`workHoursEnd` before a report can be considered to have its core fields filled (`DailyReportCoreFieldsRequiredException`). This change splits it into two independent, equally-required fields.

## Goals / Non-Goals

**Goals:** two weather values per report (morning/afternoon), both required, same 5-option set, existing data preserved.

**Non-Goals:** splitting `weatherBlockedTasks` by period (not requested); adding more than two periods; changing the weather option set itself.

## Decisions

**1. New column names: `weather_condition_morning` / `weather_condition_afternoon`** (Java: `weatherConditionMorning`/`weatherConditionAfternoon`), migration `V66__split_daily_report_weather_by_period.sql`. This migration also must drop and recreate the existing `daily_report_core_fields_together` DB-level `CHECK` constraint (added in `V61`), since it references the now-dropped `weather_condition` column directly — the recreated constraint covers both new weather columns plus work hours, same all-or-nothing shape.

**2. Existing data migration**: `UPDATE daily_report SET weather_condition_morning = weather_condition, weather_condition_afternoon = weather_condition` before dropping the old column — copies the single historical value into both periods rather than leaving one blank, so old reports don't display an incomplete-looking afternoon.

**3. Validation**: `DailyReportCoreFieldsRequiredException` is thrown when either `weatherConditionMorning` or `weatherConditionAfternoon` is null (previously just `weatherCondition`) — same all-or-nothing gate, now checking two fields instead of one.

## Risks / Trade-offs

- **[Trade-off] Historical reports show the same value for both periods** (since the real per-period split wasn't recorded at the time). Accepted — there's no way to retroactively recover morning-vs-afternoon weather that was never captured separately.

## Migration Plan

1. `V66__split_daily_report_weather_by_period.sql`: add the two new columns (nullable initially), backfill both from the old column, set both `NOT NULL`, drop `weather_condition`.
2. Backend entity/DTO/service updates.
3. Frontend + mobile: two weather pickers instead of one.
