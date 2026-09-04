CREATE TABLE daily_report_workforce_entry (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    membership_id     UUID REFERENCES project_membership (id),
    role_description  VARCHAR(255) NOT NULL,
    headcount         INT NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_workforce_entry_report ON daily_report_workforce_entry (daily_report_id);
