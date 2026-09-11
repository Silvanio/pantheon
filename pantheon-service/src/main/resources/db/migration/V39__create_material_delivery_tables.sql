-- Delivery-tracking "material": created only when an Orcamento is marked COMPLETED, one per
-- orcamento_line_item, copying its name/type/quantity at that moment. Replaces the old
-- catalog'd "material" table (dropped in V34) with the same table name but a different shape.
CREATE TABLE material (
    id                     UUID PRIMARY KEY,
    construction_site_id   UUID NOT NULL REFERENCES construction_site (id),
    orcamento_line_item_id UUID NOT NULL REFERENCES orcamento_line_item (id),
    name                   VARCHAR(255) NOT NULL,
    type                   VARCHAR(255),
    quantity               NUMERIC(19,3) NOT NULL,
    status                 VARCHAR(30) NOT NULL,
    delivered_at           TIMESTAMP WITH TIME ZONE,
    delivered_by           UUID REFERENCES app_user (id),
    checked_at             TIMESTAMP WITH TIME ZONE,
    checked_by             UUID REFERENCES app_user (id),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_material_site ON material (construction_site_id);
CREATE INDEX idx_material_orcamento_line_item ON material (orcamento_line_item_id);

CREATE TABLE material_delivery_photo (
    id             UUID PRIMARY KEY,
    material_id    UUID NOT NULL REFERENCES material (id),
    storage_key    VARCHAR(500) NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    uploaded_by    UUID NOT NULL REFERENCES app_user (id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_material_delivery_photo_material ON material_delivery_photo (material_id);
