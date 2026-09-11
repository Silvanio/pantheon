-- Company-scoped supplier registry: reused across every construction site of the same
-- Company, unlike everything else in this domain which is site-scoped. Never edited once
-- created (see orcamento-approval-workflow's find-or-create requirement) - only ever
-- inserted, then read for autocomplete and for snapshotting onto an Orcamento.
CREATE TABLE fornecedor (
    id            UUID PRIMARY KEY,
    company_id    UUID NOT NULL REFERENCES company (id),
    cnpj          VARCHAR(32) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    address       VARCHAR(500),
    contact_name  VARCHAR(255),
    contact_phone VARCHAR(50),
    created_by    UUID NOT NULL REFERENCES app_user (id),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_fornecedor_company_cnpj ON fornecedor (company_id, cnpj);
CREATE INDEX idx_fornecedor_company_cnpj_prefix ON fornecedor (company_id, cnpj varchar_pattern_ops);
