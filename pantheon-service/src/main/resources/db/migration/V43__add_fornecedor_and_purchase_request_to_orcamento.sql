-- Supplier snapshot (never a live reference - see design.md Decision 4) plus optional
-- traceability back to the Fornecedor record and to the Pedido de Compra this Orcamento was
-- converted from, if any.
ALTER TABLE orcamento
    ADD COLUMN fornecedor_cnpj VARCHAR(32),
    ADD COLUMN fornecedor_nome VARCHAR(255),
    ADD COLUMN fornecedor_endereco VARCHAR(500),
    ADD COLUMN fornecedor_contato_nome VARCHAR(255),
    ADD COLUMN fornecedor_contato_telefone VARCHAR(50),
    ADD COLUMN source_fornecedor_id UUID REFERENCES fornecedor (id),
    ADD COLUMN source_purchase_request_id UUID REFERENCES purchase_request (id);

-- No existing rows survive V40's cleanup, so backfill is unnecessary before enforcing NOT NULL.
ALTER TABLE orcamento
    ALTER COLUMN fornecedor_cnpj SET NOT NULL,
    ALTER COLUMN fornecedor_nome SET NOT NULL;

CREATE INDEX idx_orcamento_source_purchase_request ON orcamento (source_purchase_request_id);
CREATE INDEX idx_orcamento_site_created_at ON orcamento (construction_site_id, created_at);
