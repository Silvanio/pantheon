## ADDED Requirements

### Requirement: Dashboard header branding
`pantheon-web` SHALL show the active company's own logo and name in the dashboard header once a company is active, positioned flush against the header's left edge; before any company is active (e.g. during onboarding), it SHALL fall back to the generic Pantheon brand mark. The theme toggle SHALL be the rightmost control in the header, positioned after the profile menu.

#### Scenario: Company branding shown once a company is active
- **WHEN** a user with an active company opens the dashboard
- **THEN** `pantheon-web` shows that company's logo and name in the header instead of the generic Pantheon brand

#### Scenario: Generic brand shown before any company is active
- **WHEN** a user with no active company (e.g. mid-onboarding) views a header
- **THEN** `pantheon-web` shows the generic Pantheon brand mark
