-- Moves the whole approval/conclusion lifecycle from Orcamento to PurchaseRequest (see
-- openspec/changes/redesign-purchase-request-approval-and-comparison/design.md, Decisions 1-2
-- and 5). Dev-only QA rows are cleared rather than migrated into the new shape, same clean-cut
-- precedent as V40/V49 in this same domain — no production data exists to preserve.
DELETE FROM material_delivery_photo;
DELETE FROM material;
DROP TABLE orcamento_approval;
DROP TABLE site_orcamento_approval_level;
-- orcamento_line_item.source_purchase_request_item_id FKs purchase_request_item, so it must be
-- cleared before the referenced purchase_request_item rows are removed.
DELETE FROM orcamento_line_item;
DELETE FROM purchase_request_item WHERE converted_to_orcamento_id IS NOT NULL;
DELETE FROM orcamento;

-- Approval now belongs to the Pedido de Compra header: a single header can span several
-- Orcamentos/suppliers and is approved/concluded once, as one document.
CREATE TABLE purchase_request_approval (
    id                            UUID PRIMARY KEY,
    purchase_request_id           UUID NOT NULL REFERENCES purchase_request (id),
    cycle_number                  INT NOT NULL,
    step_order                    INT NOT NULL,
    approver_function             VARCHAR(30) NOT NULL,
    status                        VARCHAR(20) NOT NULL,
    decided_by_site_membership_id UUID REFERENCES site_membership (id),
    decided_at                    TIMESTAMP WITH TIME ZONE,
    comment                       VARCHAR(500),
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_purchase_request_approval_purchase_request ON purchase_request_approval (purchase_request_id);
CREATE INDEX idx_purchase_request_approval_purchase_request_cycle
    ON purchase_request_approval (purchase_request_id, cycle_number);

CREATE TABLE site_purchase_request_approval_level (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    step_order            INT NOT NULL,
    approver_function     VARCHAR(30) NOT NULL,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_purchase_request_approval_level_site ON site_purchase_request_approval_level (construction_site_id);
CREATE UNIQUE INDEX idx_site_purchase_request_approval_level_step
    ON site_purchase_request_approval_level (construction_site_id, step_order) WHERE active;

-- Header lifecycle (Iniciado/Orçado/Conferido/Concluído) plus the approval bookkeeping fields
-- that used to live on Orcamento.
ALTER TABLE purchase_request
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'INICIADO',
    ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN current_approval_cycle INT NOT NULL DEFAULT 0,
    ADD COLUMN last_rejection_reason VARCHAR(500);

ALTER TABLE purchase_request ALTER COLUMN status DROP DEFAULT;

-- Per-item supplier selection: which OrcamentoLineItem (and therefore which supplier's quote)
-- fulfills this Pedido-de-Compra item. Nullable and clearable; a single Pedido de Compra can mix
-- selections across several linked Orcamentos to buy the cheapest combination.
ALTER TABLE purchase_request_item
    ADD COLUMN selected_orcamento_line_item_id UUID REFERENCES orcamento_line_item (id);

CREATE INDEX idx_purchase_request_item_selected_line_item ON purchase_request_item (selected_orcamento_line_item_id);

-- Orcamento is reduced to Rascunho/Bloqueado, following its originating Pedido de Compra's
-- approval state instead of its own independent multi-step machine.
ALTER TABLE orcamento
    DROP COLUMN submitted_at,
    DROP COLUMN approved_at,
    DROP COLUMN completed_at,
    DROP COLUMN current_approval_cycle,
    DROP COLUMN last_rejection_reason;

-- Efficient lookup of "the line item quoted against Pedido-de-Compra item X within Orcamento Y",
-- used by the comparison table and by selection validation.
CREATE INDEX idx_orcamento_line_item_source_item ON orcamento_line_item (orcamento_id, source_purchase_request_item_id);
