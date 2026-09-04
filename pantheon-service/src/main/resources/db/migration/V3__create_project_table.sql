CREATE TABLE project (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    plan                VARCHAR(20),
    trial_started_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    trial_expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    plan_confirmed_at   TIMESTAMP WITH TIME ZONE,
    plan_valid_until    TIMESTAMP WITH TIME ZONE,
    created_by          UUID NOT NULL REFERENCES app_user (id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_project_created_by ON project (created_by);
