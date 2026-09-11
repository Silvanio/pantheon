-- Backfill: every obra's creator becomes an ADMIN site member (going forward this happens at
-- creation time in ConstructionSiteService.create). Without this, an admin never appears in
-- their own obra's team roster or as an assignable Tasks-board member. Guarded so it never
-- creates a duplicate for a site/user pair that already has a membership.
INSERT INTO site_membership (id, construction_site_id, user_id, function, status, created_at)
SELECT gen_random_uuid(), cs.id, cs.created_by, 'ADMIN', 'ACTIVE', now()
FROM construction_site cs
WHERE NOT EXISTS (
    SELECT 1 FROM site_membership sm
    WHERE sm.construction_site_id = cs.id AND sm.user_id = cs.created_by
);
