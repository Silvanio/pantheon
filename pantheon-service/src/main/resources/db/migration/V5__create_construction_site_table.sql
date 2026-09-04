CREATE TABLE construction_site (
    id                  UUID PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES project (id),
    name                VARCHAR(255) NOT NULL,
    address             VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    start_date          DATE NOT NULL,
    expected_end_date   DATE,
    created_by          UUID NOT NULL REFERENCES app_user (id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_construction_site_project ON construction_site (project_id);
