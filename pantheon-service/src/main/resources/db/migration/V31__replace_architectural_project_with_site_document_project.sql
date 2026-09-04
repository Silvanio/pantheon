-- ArchitecturalProject is generalized and its "optionally linked to a site" relationship is
-- dropped: a document project always belongs to exactly one construction site.
DROP TABLE architectural_project;

CREATE TABLE site_document_project (
    id                      UUID PRIMARY KEY,
    construction_site_id    UUID NOT NULL REFERENCES construction_site (id),
    name                    VARCHAR(255) NOT NULL,
    created_by              UUID NOT NULL REFERENCES app_user (id),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_document_project_site ON site_document_project (construction_site_id);

CREATE TABLE site_document_project_attachment (
    id                        UUID PRIMARY KEY,
    site_document_project_id  UUID NOT NULL REFERENCES site_document_project (id),
    storage_key               VARCHAR(500) NOT NULL,
    content_type              VARCHAR(100) NOT NULL,
    original_name             VARCHAR(255) NOT NULL,
    uploaded_by               UUID NOT NULL REFERENCES app_user (id),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_document_project_attachment_project ON site_document_project_attachment (site_document_project_id);
