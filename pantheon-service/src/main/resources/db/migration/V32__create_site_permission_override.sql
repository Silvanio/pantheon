-- Per-construction-site permission configuration: a row targets either one specific member
-- (site_membership_id) or every member of a function (function) for a capability, overriding
-- the hardcoded default resolved in code when no row matches.
CREATE TABLE site_permission_override (
    id                      UUID PRIMARY KEY,
    construction_site_id    UUID NOT NULL REFERENCES construction_site (id),
    site_membership_id      UUID REFERENCES site_membership (id),
    function                VARCHAR(30),
    capability              VARCHAR(30) NOT NULL,
    access_level            VARCHAR(20) NOT NULL,
    CONSTRAINT chk_site_permission_override_target
        CHECK ((site_membership_id IS NOT NULL AND function IS NULL)
            OR (site_membership_id IS NULL AND function IS NOT NULL))
);

CREATE INDEX idx_site_permission_override_site ON site_permission_override (construction_site_id);
CREATE UNIQUE INDEX idx_site_permission_override_member_capability
    ON site_permission_override (site_membership_id, capability) WHERE site_membership_id IS NOT NULL;
CREATE UNIQUE INDEX idx_site_permission_override_function_capability
    ON site_permission_override (construction_site_id, function, capability) WHERE function IS NOT NULL;
