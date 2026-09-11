-- One or more site members can be assigned to a task card. Keyed by site_membership_id (not
-- user_id) since a membership can belong to a service-provider member with no app_user account.
-- See obra-tasks-board.
CREATE TABLE task_card_assignee (
    id                   UUID PRIMARY KEY,
    card_id              UUID NOT NULL REFERENCES task_card (id),
    site_membership_id   UUID NOT NULL REFERENCES site_membership (id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_task_card_assignee ON task_card_assignee (card_id, site_membership_id);
CREATE INDEX idx_task_card_assignee_card ON task_card_assignee (card_id);
