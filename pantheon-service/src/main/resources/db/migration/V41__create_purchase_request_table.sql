-- Pedido de Compra header: groups PurchaseRequestItems entered together under one auto-named
-- document ("Pedido dd/MM/yyyy #n"). Items keep their own construction_site_id (denormalized)
-- so existing site-scoped queries need no join rewrite.
CREATE TABLE purchase_request (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    name                  VARCHAR(255) NOT NULL,
    created_by            UUID NOT NULL REFERENCES app_user (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_request_site ON purchase_request (construction_site_id);
CREATE INDEX idx_purchase_request_site_created_at ON purchase_request (construction_site_id, created_at);

ALTER TABLE purchase_request_item
    ADD COLUMN purchase_request_id UUID REFERENCES purchase_request (id);

-- No existing rows survive V40's cleanup, so backfill is unnecessary before enforcing NOT NULL.
ALTER TABLE purchase_request_item
    ALTER COLUMN purchase_request_id SET NOT NULL;

CREATE INDEX idx_purchase_request_item_purchase_request ON purchase_request_item (purchase_request_id);
