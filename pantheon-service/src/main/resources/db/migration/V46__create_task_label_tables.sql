-- Task labels are obra-scoped and reusable across that obra's cards (many-to-many via
-- task_card_label). Distinct from the global-tasks-board's per-obra color, which is computed,
-- not stored. See obra-tasks-board.
CREATE TABLE task_label (
    id                    UUID PRIMARY KEY,
    construction_site_id  UUID NOT NULL REFERENCES construction_site (id),
    name                  VARCHAR(100) NOT NULL,
    color_hex             VARCHAR(7) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_label_site ON task_label (construction_site_id);

CREATE TABLE task_card_label (
    id        UUID PRIMARY KEY,
    card_id   UUID NOT NULL REFERENCES task_card (id),
    label_id  UUID NOT NULL REFERENCES task_label (id)
);

CREATE UNIQUE INDEX uq_task_card_label ON task_card_label (card_id, label_id);
CREATE INDEX idx_task_card_label_label ON task_card_label (label_id);
