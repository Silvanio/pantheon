CREATE TABLE material (
    id                     UUID PRIMARY KEY,
    construction_site_id   UUID NOT NULL REFERENCES construction_site (id),
    name                   VARCHAR(255) NOT NULL,
    unit                   VARCHAR(50) NOT NULL,
    created_by             UUID NOT NULL REFERENCES app_user (id),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_material_construction_site ON material (construction_site_id);
