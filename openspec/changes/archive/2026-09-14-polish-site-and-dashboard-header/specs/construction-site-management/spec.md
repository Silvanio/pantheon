## ADDED Requirements

### Requirement: Collapsible obra cover photo
`pantheon-web` SHALL allow the obra detail page's cover photo to be collapsed and expanded via a toggle control. When collapsed, the photo SHALL be hidden and the obra's name SHALL appear in the page header instead, alongside an expand control; when expanded, the photo SHALL show as before, with a collapse control on it. This preference SHALL be a single global choice, not remembered separately per obra, persisted so it is remembered on the next visit to any obra. The obra detail page's header SHALL also offer the light/dark theme toggle.

#### Scenario: Member collapses the cover photo
- **WHEN** a member on an obra's detail page collapses the cover photo
- **THEN** `pantheon-web` hides the photo and shows the obra's name in the page header along with an expand control

#### Scenario: Member expands the cover photo
- **WHEN** a member on an obra's detail page with the cover photo collapsed uses the expand control
- **THEN** `pantheon-web` shows the cover photo again as before

#### Scenario: Collapse preference carries over to other obras
- **WHEN** a member collapses the cover photo on one obra and then opens a different obra
- **THEN** `pantheon-web` shows that other obra's cover photo collapsed too, without the member repeating the action

#### Scenario: Theme toggle available on the obra detail page
- **WHEN** a member is on an obra's detail page
- **THEN** `pantheon-web` offers the light/dark theme toggle in that page's header
