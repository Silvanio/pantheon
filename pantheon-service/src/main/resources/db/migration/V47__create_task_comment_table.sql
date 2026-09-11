CREATE TABLE task_comment (
    id          UUID PRIMARY KEY,
    card_id     UUID NOT NULL REFERENCES task_card (id),
    author_id   UUID NOT NULL REFERENCES app_user (id),
    body        TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_comment_card ON task_comment (card_id);
