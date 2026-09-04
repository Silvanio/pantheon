CREATE TABLE daily_report_activity (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    description       VARCHAR(500) NOT NULL,
    progress_note     VARCHAR(255) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_activity_report ON daily_report_activity (daily_report_id);
