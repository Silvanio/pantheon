CREATE TABLE material_request (
    id                     UUID PRIMARY KEY,
    construction_site_id   UUID NOT NULL REFERENCES construction_site (id),
    status                 VARCHAR(20) NOT NULL,
    requested_by           UUID NOT NULL REFERENCES app_user (id),
    decided_by             UUID REFERENCES app_user (id),
    decision_note          VARCHAR(500),
    decided_at             TIMESTAMP WITH TIME ZONE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_material_request_site ON material_request (construction_site_id);
CREATE INDEX idx_material_request_site_status ON material_request (construction_site_id, status);

CREATE TABLE material_request_item (
    id                    UUID PRIMARY KEY,
    material_request_id  UUID NOT NULL REFERENCES material_request (id),
    material_id          UUID NOT NULL REFERENCES material (id),
    requested_quantity    NUMERIC(19,3) NOT NULL
);

CREATE INDEX idx_material_request_item_request ON material_request_item (material_request_id);
