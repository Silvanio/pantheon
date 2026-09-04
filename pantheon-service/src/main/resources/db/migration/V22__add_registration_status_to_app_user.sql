-- A pre-registration account is created for an invited email that has no account yet:
-- email only, no credentials, registration_status = PENDING_REGISTRATION. It cannot log
-- in until the invited person completes registration. Nullable + backfill so existing
-- accounts are treated as ACTIVE.
ALTER TABLE app_user
    ADD COLUMN registration_status VARCHAR(20);

UPDATE app_user SET registration_status = 'ACTIVE' WHERE registration_status IS NULL;
