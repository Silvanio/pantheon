## MODIFIED Requirements

### Requirement: List project members
`pantheon-service` SHALL allow any active member of a project to list that project's members, including each member's role, construction function, specialty, and invitation state. The list SHALL include memberships that are still `INVITED` (not yet accepted) alongside active members, with a field distinguishing the two.

#### Scenario: Member lists project members
- **WHEN** an authenticated active member of a project requests the project's member list
- **THEN** `pantheon-service` returns every membership of the project with its email, role, construction function, specialty, and whether it is still an unaccepted invitation

#### Scenario: Invited members appear as invitations
- **WHEN** a project has members whose memberships are `INVITED`
- **THEN** those entries are returned in the member list marked as invitations

#### Scenario: Non-member cannot list project members
- **WHEN** a user who is not an active member of the project (including a user with only an `INVITED` membership) requests its member list
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Construction site management views
`pantheon-web` SHALL provide views for listing and creating `ConstructionSite`s under a project, a form for registering `ArchitecturalProject`s, a construction-function/specialty selector in the add-member flow, and a project member list showing each member's role, function, specialty, and — for members who have not yet accepted — a "Convite" indicator. The add-member flow SHALL work with an email that has no account yet.

#### Scenario: Admin creates a construction site from the UI
- **WHEN** a project administrator submits the construction site creation form with a valid name, address, and start date
- **THEN** `pantheon-web` submits the data to `pantheon-service` and, on success, shows the new site in the project's site list

#### Scenario: Admin adds a member with a function from the UI
- **WHEN** a project administrator submits the add-member form with a selected construction function (and specialty, if Service Provider)
- **THEN** `pantheon-web` submits the function (and specialty) alongside the existing add-member data

#### Scenario: Invited member shows a Convite badge
- **WHEN** the member list contains a member whose invitation has not been accepted
- **THEN** `pantheon-web` displays that member in the list with a "Convite" badge next to their name, and removes the badge once the invitation is accepted

#### Scenario: Admin invites an email with no account
- **WHEN** a project administrator submits the add-member form with an email that is not yet registered
- **THEN** `pantheon-web` submits it successfully and shows the person in the member list with a "Convite" badge
