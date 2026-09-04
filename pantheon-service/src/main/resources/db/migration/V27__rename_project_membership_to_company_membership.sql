-- CompanyMembership is now narrowed to company staff only (role ADMIN/MEMBER); construction
-- functions move to the new site_membership table (see V29), scoped to a construction site.
ALTER TABLE project_membership RENAME TO company_membership;
ALTER TABLE company_membership RENAME COLUMN project_id TO company_id;
ALTER INDEX idx_project_membership_project_user RENAME TO idx_company_membership_company_user;
ALTER INDEX idx_project_membership_user RENAME TO idx_company_membership_user;

ALTER TABLE company_membership
    DROP COLUMN function,
    DROP COLUMN specialty;
