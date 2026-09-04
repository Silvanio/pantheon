CREATE TABLE project_membership (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL REFERENCES project (id),
    user_id     UUID NOT NULL REFERENCES app_user (id),
    role        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_project_membership_project_user ON project_membership (project_id, user_id);
CREATE INDEX idx_project_membership_user ON project_membership (user_id);
