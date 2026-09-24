-- Weather condition, work hours start, and work hours end are mandatory together once a daily
-- report's core section is saved (only comments stay optional) — see
-- DailyReportService.updateCore's application-level check for the actual "must all be present"
-- rule. This constraint is the DB-level backstop: it can't tell "freshly created, not yet
-- saved" apart from "saved with data missing" (both look like nulls), so it only rejects the
-- one state application code should never produce — some but not all three set — leaving the
-- freshly-created all-null state and the fully-saved all-set state both valid.
-- NOT VALID: applies to all new inserts/updates without failing on existing rows that predate
-- this rule.
ALTER TABLE daily_report
    ADD CONSTRAINT daily_report_core_fields_together CHECK (
        (weather_condition IS NULL AND work_hours_start IS NULL AND work_hours_end IS NULL)
        OR (weather_condition IS NOT NULL AND work_hours_start IS NOT NULL AND work_hours_end IS NOT NULL)
    ) NOT VALID;
