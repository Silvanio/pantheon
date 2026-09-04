-- Invitations now target either a company_membership or a site_membership row. The token/
-- email/accept mechanics are identical regardless of target, so one table keeps serving both
-- via a membership_type discriminator instead of duplicating the invitation machinery.
ALTER TABLE membership_invitation DROP CONSTRAINT IF EXISTS membership_invitation_membership_id_fkey;
ALTER TABLE membership_invitation ADD COLUMN membership_type VARCHAR(20) NOT NULL DEFAULT 'COMPANY';
ALTER TABLE membership_invitation ALTER COLUMN membership_type DROP DEFAULT;
