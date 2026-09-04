-- Membership lifecycle: a member added by an admin starts as INVITED (no project access)
-- and becomes ACTIVE only when the invited person accepts. Nullable + backfill so rows
-- created before invitations existed are treated as ACTIVE.
ALTER TABLE project_membership
    ADD COLUMN status VARCHAR(20);

UPDATE project_membership SET status = 'ACTIVE' WHERE status IS NULL;
