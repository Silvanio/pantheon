-- Rescope task_label away from construction_site_id: it now splits into company-scoped
-- predefined labels (company_id set) and card-only custom labels (card_id set). Existing rows
-- are disposable dev-only data (same precedent as V40's purchase-request/orcamento clear), so
-- they're cleared rather than backfilled. See obra-tasks-board / company-task-labels.
DELETE FROM task_card_label;
DELETE FROM task_label;

ALTER TABLE task_label DROP COLUMN construction_site_id;
ALTER TABLE task_label ADD COLUMN company_id UUID REFERENCES company (id);
ALTER TABLE task_label ADD COLUMN card_id UUID REFERENCES task_card (id);
ALTER TABLE task_label ADD CONSTRAINT chk_task_label_scope CHECK (
    (company_id IS NOT NULL AND card_id IS NULL) OR (company_id IS NULL AND card_id IS NOT NULL)
);

CREATE INDEX idx_task_label_company ON task_label (company_id);
CREATE INDEX idx_task_label_card ON task_label (card_id);

-- Every company gets a predefined "Urgente" label from the start.
INSERT INTO task_label (id, company_id, name, color_hex, created_at)
SELECT gen_random_uuid(), c.id, 'Urgente', '#EF4444', now() FROM company c;
