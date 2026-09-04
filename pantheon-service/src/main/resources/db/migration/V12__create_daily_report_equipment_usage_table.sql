CREATE TABLE daily_report_equipment_usage (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    equipment_id      UUID NOT NULL REFERENCES equipment (id),
    status_note       VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_equipment_usage_report ON daily_report_equipment_usage (daily_report_id);
