-- Diário de Obra approval workflow (see
-- openspec/changes/add-daily-report-approval-workflow): DailyReportStatus grows from
-- {DRAFT, SUBMITTED} to {DRAFT, PENDING_APPROVAL, APPROVED}. A report that was "submitted"
-- under the old 2-status model was already effectively finalized, so existing SUBMITTED rows
-- become APPROVED before the new status constraint is added — no retroactive DailyReportApproval
-- history is created for them (see design.md Decision 6).
UPDATE daily_report SET status = 'APPROVED' WHERE status = 'SUBMITTED';

ALTER TABLE daily_report
    ADD CONSTRAINT daily_report_status_check CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED'));

-- Tracks the report's current approval cycle, mirroring purchase_request.current_approval_cycle:
-- incremented every time it's (re)submitted, so daily_report_approval rows from an earlier,
-- rejected cycle stay distinguishable from the live one.
ALTER TABLE daily_report
    ADD COLUMN current_approval_cycle INT NOT NULL DEFAULT 0;

-- Per-site configurable Diário de Obra approval chain — mirrors
-- site_purchase_request_approval_level's columns field-for-field. New, parallel table rather
-- than a shared one (see design.md Decision 1): this change does not touch Pedido de Compra's
-- existing table or data.
CREATE TABLE site_daily_report_approval_level (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    step_order            INT NOT NULL,
    approver_function     VARCHAR(30) NOT NULL,
    active                BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_daily_report_approval_level_site ON site_daily_report_approval_level (construction_site_id);
CREATE UNIQUE INDEX idx_site_daily_report_approval_level_step
    ON site_daily_report_approval_level (construction_site_id, step_order) WHERE active;

-- One row per approval step per submission cycle of a Diário de Obra, snapshotted from
-- site_daily_report_approval_level (or the default) at submit time — mirrors
-- purchase_request_approval's columns field-for-field. Rows from earlier, rejected cycles are
-- kept for history rather than deleted or overwritten.
CREATE TABLE daily_report_approval (
    id                            UUID PRIMARY KEY,
    daily_report_id               UUID NOT NULL REFERENCES daily_report (id),
    cycle_number                  INT NOT NULL,
    step_order                    INT NOT NULL,
    approver_function             VARCHAR(30) NOT NULL,
    status                        VARCHAR(20) NOT NULL,
    decided_by_site_membership_id UUID REFERENCES site_membership (id),
    decided_at                    TIMESTAMP WITH TIME ZONE,
    comment                       VARCHAR(500),
    created_at                    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_daily_report_approval_report ON daily_report_approval (daily_report_id);
CREATE INDEX idx_daily_report_approval_report_cycle ON daily_report_approval (daily_report_id, cycle_number);
