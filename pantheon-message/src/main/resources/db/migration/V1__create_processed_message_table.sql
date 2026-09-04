CREATE TABLE processed_message (
    id              UUID PRIMARY KEY,
    correlation_id  UUID NOT NULL,
    type            VARCHAR(255) NOT NULL,
    payload         TEXT NOT NULL,
    occurred_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_message_correlation_id ON processed_message (correlation_id);
