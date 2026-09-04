CREATE TABLE daily_report_signature (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    membership_id     UUID NOT NULL REFERENCES project_membership (id),
    function          VARCHAR(30),
    signed_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_signature_report ON daily_report_signature (daily_report_id);
