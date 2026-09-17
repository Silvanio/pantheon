## MODIFIED Requirements

### Requirement: File upload
`pantheon-service` SHALL allow a member with `MANAGE` access to `DOCUMENT_PROJECTS` to upload a non-empty file directly into Projetos, either at the construction site's root or inside an existing folder on that site, stored in object storage under a key scoped to that site. Only files whose original filename extension (case-insensitive) is `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4` SHALL be accepted; any other extension SHALL be rejected. A `.mp4` file SHALL additionally be rejected if its duration exceeds 60 seconds, or if its duration cannot be determined.

#### Scenario: File uploaded into a folder
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` uploads a file naming an existing folder as its destination
- **THEN** `pantheon-service` stores the file and records a `SiteDocumentProjectAttachment` nested under that folder

#### Scenario: File uploaded at the site root
- **WHEN** a member with `MANAGE` access to `DOCUMENT_PROJECTS` uploads a file naming no destination folder
- **THEN** `pantheon-service` stores the file at the construction site's root

#### Scenario: Empty file rejected
- **WHEN** a member attempts to upload an empty file
- **THEN** `pantheon-service` rejects the upload

#### Scenario: View-only member cannot upload directly
- **WHEN** a construction site member whose `DOCUMENT_PROJECTS` access is `VIEW` attempts to upload a file directly into Projetos
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Disallowed file type rejected
- **WHEN** a member attempts to upload a file whose extension is not `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4`
- **THEN** `pantheon-service` rejects the upload

#### Scenario: Allowed file types accepted
- **WHEN** a member uploads a file with extension `.jpg`, `.jpeg`, `.png`, `.pdf`, or `.mp4`
- **THEN** `pantheon-service` accepts and stores it

#### Scenario: Video within the duration cap accepted
- **WHEN** a member uploads an `.mp4` file whose duration is 60 seconds or less
- **THEN** `pantheon-service` accepts and stores it

#### Scenario: Video exceeding the duration cap rejected
- **WHEN** a member uploads an `.mp4` file whose duration exceeds 60 seconds
- **THEN** `pantheon-service` rejects the upload

#### Scenario: Video with undeterminable duration rejected
- **WHEN** a member uploads an `.mp4` file whose duration `pantheon-service` cannot determine
- **THEN** `pantheon-service` rejects the upload
