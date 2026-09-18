-- A fiscal invoice ("nota fiscal") attached to a Pedido de Compra, at any point in its lifecycle.
CREATE TABLE purchase_request_invoice (
    id                  UUID PRIMARY KEY,
    purchase_request_id UUID NOT NULL REFERENCES purchase_request (id),
    storage_key         VARCHAR(500) NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    original_name       VARCHAR(255) NOT NULL,
    uploaded_by         UUID NOT NULL REFERENCES app_user (id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_request_invoice_purchase_request ON purchase_request_invoice (purchase_request_id);
