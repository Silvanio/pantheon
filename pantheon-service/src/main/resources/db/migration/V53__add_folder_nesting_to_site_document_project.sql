-- site_document_project becomes a self-referencing folder tree (parent_id = null is the
-- construction site's root) and can optionally link to a task_card, cleared (never cascaded)
-- when that task is deleted. See redesign-site-projects-as-folder-explorer's design.md.
ALTER TABLE site_document_project
    ADD COLUMN parent_id    UUID REFERENCES site_document_project (id) ON DELETE CASCADE,
    ADD COLUMN task_card_id UUID REFERENCES task_card (id) ON DELETE SET NULL,
    ADD COLUMN updated_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN updated_by   UUID REFERENCES app_user (id);

UPDATE site_document_project SET updated_at = created_at, updated_by = created_by;

ALTER TABLE site_document_project
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL;

CREATE INDEX idx_site_document_project_parent ON site_document_project (parent_id);
CREATE INDEX idx_site_document_project_task_card ON site_document_project (task_card_id);

-- Files can now sit at a site's root (site_document_project_id = null), so they carry their own
-- construction_site_id instead of relying on a join through the (now optional) parent folder.
-- They can also optionally link to a task_card, the only way that link is ever created.
ALTER TABLE site_document_project_attachment
    ADD COLUMN construction_site_id UUID REFERENCES construction_site (id),
    ADD COLUMN task_card_id         UUID REFERENCES task_card (id) ON DELETE SET NULL;

UPDATE site_document_project_attachment a
SET construction_site_id = p.construction_site_id
FROM site_document_project p
WHERE a.site_document_project_id = p.id;

ALTER TABLE site_document_project_attachment
    ALTER COLUMN construction_site_id SET NOT NULL,
    ALTER COLUMN site_document_project_id DROP NOT NULL;

-- Re-point the existing FK to cascade: deleting a folder deletes the file rows nested under it
-- (their storage objects are purged by the service beforehand — see design.md decision 3).
ALTER TABLE site_document_project_attachment
    DROP CONSTRAINT site_document_project_attachment_site_document_project_id_fkey;
ALTER TABLE site_document_project_attachment
    ADD CONSTRAINT site_document_project_attachment_site_document_project_id_fkey
        FOREIGN KEY (site_document_project_id) REFERENCES site_document_project (id) ON DELETE CASCADE;

CREATE INDEX idx_site_document_project_attachment_site ON site_document_project_attachment (construction_site_id);
CREATE INDEX idx_site_document_project_attachment_task_card ON site_document_project_attachment (task_card_id);
