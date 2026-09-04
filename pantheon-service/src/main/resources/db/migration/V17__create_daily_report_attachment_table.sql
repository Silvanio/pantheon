CREATE TABLE daily_report_attachment (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    storage_key       VARCHAR(500) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    original_name     VARCHAR(255) NOT NULL,
    uploaded_by       UUID NOT NULL REFERENCES app_user (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_attachment_report ON daily_report_attachment (daily_report_id);
