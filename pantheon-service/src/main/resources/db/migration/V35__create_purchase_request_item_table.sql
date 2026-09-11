-- converted_to_orcamento_id references "orcamento", created in the next migration - the FK
-- constraint for it is added there (V36) once that table exists, to avoid a circular
-- forward-reference between purchase_request_item and orcamento.
CREATE TABLE purchase_request_item (
    id                        UUID PRIMARY KEY,
    construction_site_id      UUID NOT NULL REFERENCES construction_site (id),
    name                      VARCHAR(255) NOT NULL,
    type                      VARCHAR(255),
    quantity                  NUMERIC(19,3) NOT NULL,
    unit                      VARCHAR(50),
    status                    VARCHAR(20) NOT NULL,
    created_by                UUID NOT NULL REFERENCES app_user (id),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    converted_to_orcamento_id UUID,
    converted_at              TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_purchase_request_item_site ON purchase_request_item (construction_site_id);
CREATE INDEX idx_purchase_request_item_site_status ON purchase_request_item (construction_site_id, status);
