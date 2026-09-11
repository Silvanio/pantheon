-- A site's configured, ordered Orcamento approval chain. No rows = the single-ENGINEER
-- default resolved in code (SiteOrcamentoApprovalLevelService), so existing/unconfigured
-- obras keep working unchanged.
CREATE TABLE site_orcamento_approval_level (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    step_order            INT NOT NULL,
    approver_function     VARCHAR(30) NOT NULL,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_orcamento_approval_level_site ON site_orcamento_approval_level (construction_site_id);
CREATE UNIQUE INDEX idx_site_orcamento_approval_level_step
    ON site_orcamento_approval_level (construction_site_id, step_order) WHERE active;
