CREATE TABLE device_token (
    id                     UUID PRIMARY KEY,
    user_id                UUID NOT NULL REFERENCES app_user (id),
    platform               VARCHAR(20) NOT NULL,
    token                  VARCHAR(512) NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_token_token UNIQUE (token)
);

CREATE INDEX idx_device_token_user ON device_token (user_id);
