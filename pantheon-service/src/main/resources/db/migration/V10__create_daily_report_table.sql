CREATE TABLE daily_report (
    id                      UUID PRIMARY KEY,
    construction_site_id    UUID NOT NULL REFERENCES construction_site (id),
    report_date             DATE NOT NULL,
    sequence_no             INT NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    weather_condition       VARCHAR(255),
    weather_blocked_tasks   BOOLEAN,
    work_hours_start        TIME,
    work_hours_end          TIME,
    comments                TEXT,
    created_by              UUID NOT NULL REFERENCES app_user (id),
    submitted_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_daily_report_site_date ON daily_report (construction_site_id, report_date);
