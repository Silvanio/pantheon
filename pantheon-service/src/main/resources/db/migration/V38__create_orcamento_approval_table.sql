-- One row per approval step per submission cycle of an Orcamento, snapshotted from
-- site_orcamento_approval_level (or the default) at submit time. Rows from earlier, rejected
-- cycles are kept for history rather than deleted or overwritten.
CREATE TABLE orcamento_approval (
    id                            UUID PRIMARY KEY,
    orcamento_id                  UUID NOT NULL REFERENCES orcamento (id),
    cycle_number                  INT NOT NULL,
    step_order                    INT NOT NULL,
    approver_function             VARCHAR(30) NOT NULL,
    status                        VARCHAR(20) NOT NULL,
    decided_by_site_membership_id UUID REFERENCES site_membership (id),
    decided_at                    TIMESTAMP WITH TIME ZONE,
    comment                       VARCHAR(500),
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_orcamento_approval_orcamento ON orcamento_approval (orcamento_id);
CREATE INDEX idx_orcamento_approval_orcamento_cycle ON orcamento_approval (orcamento_id, cycle_number);
