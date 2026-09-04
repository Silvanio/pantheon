-- Project is repurposed as Company: the paying tenant. Renamed in place rather than
-- introduced as a parallel table since nothing needs both concepts to coexist.
ALTER TABLE project RENAME TO company;
ALTER INDEX idx_project_created_by RENAME TO idx_company_created_by;
