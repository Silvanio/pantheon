-- A construction site's own team, independent of company staff (company_membership).
-- user_id is nullable: a SERVICE_PROVIDER row may have no account/login (display_name/
-- contact_email hold the identity instead); every other function requires an account and
-- goes through the invite/accept lifecycle (status INVITED/ACTIVE), same as company_membership.
CREATE TABLE site_membership (
    id                      UUID PRIMARY KEY,
    construction_site_id    UUID NOT NULL REFERENCES construction_site (id),
    user_id                 UUID REFERENCES app_user (id),
    function                VARCHAR(30) NOT NULL,
    service_provider_trade  VARCHAR(255),
    status                  VARCHAR(20) NOT NULL,
    client_cpf              VARCHAR(20),
    display_name            VARCHAR(255),
    contact_email           VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_membership_site ON site_membership (construction_site_id);
CREATE INDEX idx_site_membership_user ON site_membership (user_id);
