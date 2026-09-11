-- Task cards are obra-scoped: each construction site has its own independent set of cards,
-- placed into the columns shared by its company (task_column). See obra-tasks-board.
CREATE TABLE task_card (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    column_id             UUID NOT NULL REFERENCES task_column (id),
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    sort_order            INTEGER NOT NULL,
    created_by            UUID NOT NULL REFERENCES app_user (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_card_site ON task_card (construction_site_id);
CREATE INDEX idx_task_card_column ON task_card (column_id);
