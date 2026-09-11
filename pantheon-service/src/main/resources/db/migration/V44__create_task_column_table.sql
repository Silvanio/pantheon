-- Company-scoped Tasks board columns: shared by every construction site of the same Company,
-- configured only by a company admin (see company-task-columns). Not site-scoped, unlike
-- everything below it (task_card, task_label, task_comment).
CREATE TABLE task_column (
    id          UUID PRIMARY KEY,
    company_id  UUID NOT NULL REFERENCES company (id),
    name        VARCHAR(255) NOT NULL,
    sort_order  INTEGER NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_column_company ON task_column (company_id);
CREATE INDEX idx_task_column_company_sort_order ON task_column (company_id, sort_order);
