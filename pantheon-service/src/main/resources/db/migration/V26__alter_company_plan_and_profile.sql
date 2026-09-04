-- Plan choice moves from an inline enum column to a FK into the registered plan catalog;
-- the trial-period columns are dropped since a plan must be chosen immediately at signup
-- instead of reactively after a time-boxed trial expires. The company also gains its own
-- commercial-profile fields (distinct from a person's own AppUser/UserProfile data).
ALTER TABLE company
    ADD COLUMN plan_id UUID REFERENCES plan (id),
    ADD COLUMN legal_name VARCHAR(255),
    ADD COLUMN trade_name VARCHAR(255),
    ADD COLUMN cnpj VARCHAR(20),
    ADD COLUMN address VARCHAR(255),
    ADD COLUMN logo_object_key VARCHAR(500);

UPDATE company c
SET plan_id = p.id
FROM plan p
WHERE c.plan = 'BASIC' AND p.code = 'BASIC';

UPDATE company c
SET plan_id = p.id
FROM plan p
WHERE c.plan = 'PRO' AND p.code = 'PROFISSIONAL';

UPDATE company c
SET plan_id = p.id
FROM plan p
WHERE c.plan = 'UNLIMITED' AND p.code = 'ILIMITADO';

ALTER TABLE company
    DROP COLUMN plan,
    DROP COLUMN trial_started_at,
    DROP COLUMN trial_expires_at,
    DROP COLUMN plan_confirmed_at,
    DROP COLUMN plan_valid_until;
