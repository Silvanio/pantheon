-- Client-facing budget/quote drafted against a MaterialRequest. A request may have more than
-- one Orcamento over time (e.g., a revised quote after a rejection), so this is a separate
-- entity rather than fields bolted onto material_request.
CREATE TABLE orcamento (
    id                    UUID PRIMARY KEY,
    material_request_id  UUID NOT NULL REFERENCES material_request (id),
    status                VARCHAR(20) NOT NULL,
    created_by            UUID NOT NULL REFERENCES app_user (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at               TIMESTAMP WITH TIME ZONE,
    decided_at            TIMESTAMP WITH TIME ZONE,
    rejection_reason      VARCHAR(500)
);

CREATE INDEX idx_orcamento_request ON orcamento (material_request_id);

CREATE TABLE orcamento_line_item (
    id                       UUID PRIMARY KEY,
    orcamento_id             UUID NOT NULL REFERENCES orcamento (id),
    material_request_item_id UUID NOT NULL REFERENCES material_request_item (id),
    unit_price               NUMERIC(19,2) NOT NULL
);

CREATE INDEX idx_orcamento_line_item_orcamento ON orcamento_line_item (orcamento_id);

CREATE TABLE orcamento_attachment (
    id                UUID PRIMARY KEY,
    orcamento_id      UUID NOT NULL REFERENCES orcamento (id),
    kind              VARCHAR(20) NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    original_name     VARCHAR(255) NOT NULL,
    uploaded_by       UUID NOT NULL REFERENCES app_user (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_orcamento_attachment_orcamento ON orcamento_attachment (orcamento_id);

-- Delivery-proof photos on an existing receipt verification.
CREATE TABLE receipt_verification_photo (
    id                       UUID PRIMARY KEY,
    receipt_verification_id UUID NOT NULL REFERENCES receipt_verification (id),
    storage_key              VARCHAR(500) NOT NULL,
    content_type             VARCHAR(100) NOT NULL,
    uploaded_by              UUID NOT NULL REFERENCES app_user (id),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_receipt_verification_photo_verification ON receipt_verification_photo (receipt_verification_id);
