-- One invitation per INVITED membership. Only a SHA-256 hash of the token is stored;
-- the raw token travels only in the invitation email. requires_registration is true when
-- the invited email had no account and a pre-registration one was created.
CREATE TABLE membership_invitation (
    id                    UUID PRIMARY KEY,
    membership_id         UUID NOT NULL UNIQUE REFERENCES project_membership (id),
    email                 VARCHAR(255) NOT NULL,
    token_hash            VARCHAR(255) NOT NULL UNIQUE,
    invited_by            UUID NOT NULL REFERENCES app_user (id),
    requires_registration BOOLEAN NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at           TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_membership_invitation_email ON membership_invitation (email);
