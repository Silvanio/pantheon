-- Rebuilt orcamento shape: site-scoped (no more material_request_id), multi-level approval
-- (current_approval_cycle + orcamento_approval, created in the next migrations),
-- Rascunho/Em aprovação/Aprovado/Concluído lifecycle.
CREATE TABLE orcamento (
    id                       UUID PRIMARY KEY,
    construction_site_id     UUID NOT NULL REFERENCES construction_site (id),
    status                   VARCHAR(20) NOT NULL,
    created_by               UUID NOT NULL REFERENCES app_user (id),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    submitted_at             TIMESTAMP WITH TIME ZONE,
    approved_at              TIMESTAMP WITH TIME ZONE,
    completed_at             TIMESTAMP WITH TIME ZONE,
    current_approval_cycle   INT NOT NULL DEFAULT 0,
    last_rejection_reason    VARCHAR(500)
);

CREATE INDEX idx_orcamento_site ON orcamento (construction_site_id);

ALTER TABLE purchase_request_item
    ADD CONSTRAINT fk_purchase_request_item_orcamento FOREIGN KEY (converted_to_orcamento_id) REFERENCES orcamento (id);

-- Free-text line items (name/type/quantity/unit_price) - never a catalog reference.
-- source_purchase_request_item_id is kept purely for traceability when converted from a
-- Pedido de Compra selection; the line item's own data is always an independent copy.
CREATE TABLE orcamento_line_item (
    id                              UUID PRIMARY KEY,
    orcamento_id                    UUID NOT NULL REFERENCES orcamento (id),
    name                            VARCHAR(255) NOT NULL,
    type                            VARCHAR(255),
    quantity                        NUMERIC(19,3) NOT NULL,
    unit_price                      NUMERIC(19,2),
    source_purchase_request_item_id UUID REFERENCES purchase_request_item (id)
);

CREATE INDEX idx_orcamento_line_item_orcamento ON orcamento_line_item (orcamento_id);
