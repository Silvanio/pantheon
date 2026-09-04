CREATE TABLE architectural_project (
    id                     UUID PRIMARY KEY,
    project_id             UUID NOT NULL REFERENCES project (id),
    construction_site_id   UUID REFERENCES construction_site (id),
    name                   VARCHAR(255) NOT NULL,
    description            TEXT,
    created_by             UUID NOT NULL REFERENCES app_user (id),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_architectural_project_project ON architectural_project (project_id);
CREATE INDEX idx_architectural_project_site ON architectural_project (construction_site_id);
