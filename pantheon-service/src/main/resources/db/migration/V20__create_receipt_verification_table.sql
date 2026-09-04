CREATE TABLE receipt_verification (
    id                        UUID PRIMARY KEY,
    material_request_item_id  UUID NOT NULL REFERENCES material_request_item (id),
    received_quantity         NUMERIC(19,3) NOT NULL,
    verified_by               UUID NOT NULL REFERENCES app_user (id),
    verified_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    note                       VARCHAR(500)
);

CREATE UNIQUE INDEX idx_receipt_verification_item ON receipt_verification (material_request_item_id);
