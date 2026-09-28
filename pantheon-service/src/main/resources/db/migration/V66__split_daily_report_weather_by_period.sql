ALTER TABLE daily_report ADD COLUMN weather_condition_morning VARCHAR(20);
ALTER TABLE daily_report ADD COLUMN weather_condition_afternoon VARCHAR(20);

UPDATE daily_report
SET weather_condition_morning = weather_condition,
    weather_condition_afternoon = weather_condition
WHERE weather_condition IS NOT NULL;

-- Replaces daily_report_core_fields_together (V61), which referenced the now-dropped
-- weather_condition column, with the same all-or-nothing rule across the two new weather
-- columns plus work hours.
ALTER TABLE daily_report DROP CONSTRAINT daily_report_core_fields_together;

ALTER TABLE daily_report DROP COLUMN weather_condition;

ALTER TABLE daily_report
    ADD CONSTRAINT daily_report_core_fields_together CHECK (
        (weather_condition_morning IS NULL AND weather_condition_afternoon IS NULL
            AND work_hours_start IS NULL AND work_hours_end IS NULL)
        OR (weather_condition_morning IS NOT NULL AND weather_condition_afternoon IS NOT NULL
            AND work_hours_start IS NOT NULL AND work_hours_end IS NOT NULL)
    ) NOT VALID;
