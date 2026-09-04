ALTER TABLE construction_site RENAME COLUMN project_id TO company_id;
ALTER INDEX idx_construction_site_project RENAME TO idx_construction_site_company;
ALTER TABLE construction_site ADD COLUMN photo_object_key VARCHAR(500);
