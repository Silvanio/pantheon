## MODIFIED Requirements

### Requirement: PDF export
`pantheon-service` SHALL generate and return a PDF document of a daily report on request, opening with a branded header (the company's logo, when configured, and name, and the construction site's name), followed by all of the report's sections, media, attachments, and signatures.

#### Scenario: PDF generated
- **WHEN** a project member requests the PDF export of a daily report
- **THEN** `pantheon-service` generates and returns a PDF reflecting that report's current sections, media, attachments, and signatures

#### Scenario: PDF opens with the company's branding
- **WHEN** a project member requests the PDF export of a daily report for a company that has a logo configured
- **THEN** `pantheon-service`'s PDF opens with that logo, the company's name, and the construction site's name
