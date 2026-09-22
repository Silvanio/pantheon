CREATE TABLE schedule_stage (
    id                      UUID PRIMARY KEY,
    construction_site_id    UUID NOT NULL REFERENCES construction_site (id),
    name                    VARCHAR(255) NOT NULL,
    color                   VARCHAR(30) NOT NULL,
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    sort_order              INTEGER NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedule_stage_site ON schedule_stage (construction_site_id);

CREATE TABLE schedule_task (
    id                              UUID PRIMARY KEY,
    stage_id                        UUID NOT NULL REFERENCES schedule_stage (id),
    title                           VARCHAR(255) NOT NULL,
    start_date                      DATE NOT NULL,
    end_date                        DATE NOT NULL,
    responsible_site_membership_id  UUID REFERENCES site_membership (id),
    percent_complete                INTEGER NOT NULL DEFAULT 0,
    sort_order                      INTEGER NOT NULL,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_schedule_task_stage ON schedule_task (stage_id);

CREATE TABLE schedule_task_dependency (
    id                    UUID PRIMARY KEY,
    predecessor_task_id   UUID NOT NULL REFERENCES schedule_task (id),
    successor_task_id     UUID NOT NULL REFERENCES schedule_task (id),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_schedule_task_dependency UNIQUE (predecessor_task_id, successor_task_id)
);

CREATE INDEX idx_schedule_task_dependency_predecessor ON schedule_task_dependency (predecessor_task_id);
CREATE INDEX idx_schedule_task_dependency_successor ON schedule_task_dependency (successor_task_id);
