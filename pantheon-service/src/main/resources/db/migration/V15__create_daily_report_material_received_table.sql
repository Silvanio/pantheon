CREATE TABLE daily_report_material_received (
    id                UUID PRIMARY KEY,
    daily_report_id   UUID NOT NULL REFERENCES daily_report (id),
    material_id       UUID NOT NULL REFERENCES material (id),
    quantity          NUMERIC(19,3) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_material_received_report ON daily_report_material_received (daily_report_id);
