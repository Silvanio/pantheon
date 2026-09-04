## ADDED Requirements

### Requirement: Document project creation
`pantheon-service` SHALL allow a member of a construction site with `MANAGE` access to that site's document projects to create a `SiteDocumentProject`, recording a name, the creation date, and the creating member, linked to that construction site.

#### Scenario: Member creates a document project
- **WHEN** a construction site member with document-project management access submits a name for a new document project
- **THEN** `pantheon-service` persists a new `SiteDocumentProject` linked to that site, recording the creation date and the creating member

#### Scenario: View-only member cannot create
- **WHEN** a construction site member whose document-project access is `VIEW` attempts to create a document project
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: PDF attachment upload
`pantheon-service` SHALL allow a member with `MANAGE` access to a document project's site to attach one or more PDF files to it, stored in object storage under that project's key path.

#### Scenario: PDF attached
- **WHEN** a member with management access uploads a PDF file to a document project
- **THEN** `pantheon-service` stores the file in object storage and records a `SiteDocumentProjectAttachment` referencing it and its original filename

#### Scenario: Non-PDF rejected
- **WHEN** a member attempts to attach a file that is not a PDF to a document project
- **THEN** `pantheon-service` rejects the upload

### Requirement: List and view document projects
`pantheon-service` SHALL allow any member of a construction site to list its document projects and view one's detail, including its attachments.

#### Scenario: Member lists document projects
- **WHEN** a member of a construction site requests the list of its document projects
- **THEN** `pantheon-service` returns every `SiteDocumentProject` for that site, including its creation date and creating member

#### Scenario: Member views a document project's attachments
- **WHEN** a member of a construction site requests a specific document project's detail
- **THEN** `pantheon-service` returns the project along with every attached PDF's metadata

### Requirement: Document project views
`pantheon-web` SHALL provide, within a construction site's menu, a "Projetos" view listing document projects with creation date and creator, a creation form (visible only to members with management access), and a detail view with PDF upload and download, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a document project from the UI
- **WHEN** a member with management access submits the document project creation form
- **THEN** `pantheon-web` submits it to `pantheon-service` and shows the new project in the site's "Projetos" list

#### Scenario: Member uploads a PDF from the UI
- **WHEN** a member with management access selects a PDF file and uploads it from a document project's detail view
- **THEN** `pantheon-web` submits it to `pantheon-service` and adds it to the project's attachment list
