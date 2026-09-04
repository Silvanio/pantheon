-- Plans become registered data instead of a hardcoded three-way choice, so the
-- per-plan active-construction-site limit can change without a code deploy.
CREATE TABLE plan (
    id                  UUID PRIMARY KEY,
    code                VARCHAR(20) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    active_site_limit   INTEGER,
    sort_order          INTEGER NOT NULL
);

-- Fixed literal ids (not gen_random_uuid()) so the catalog's identity is stable and
-- does not depend on a Postgres version/extension providing that function.
INSERT INTO plan (id, code, name, active_site_limit, sort_order) VALUES
    ('88fbe3d7-60b5-4672-9013-48118a0c8090', 'BASIC', 'Basic', 2, 1),
    ('e4b85991-61d7-4401-9de0-5fb467fb8953', 'PROFISSIONAL', 'Profissional', 10, 2),
    ('049149e0-94ce-42ca-8b60-126c30b64a4a', 'ILIMITADO', 'Ilimitado', NULL, 3);
