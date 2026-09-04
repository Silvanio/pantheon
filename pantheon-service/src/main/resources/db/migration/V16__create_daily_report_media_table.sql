CREATE TABLE daily_report_media (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    type              VARCHAR(20) NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    caption           VARCHAR(255),
    uploaded_by       UUID NOT NULL REFERENCES app_user (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_media_report ON daily_report_media (daily_report_id);
