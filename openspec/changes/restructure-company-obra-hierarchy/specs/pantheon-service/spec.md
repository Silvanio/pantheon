## MODIFIED Requirements

### Requirement: Project creation
`pantheon-service` SHALL allow an authenticated user with no company to create a `Company` by supplying a company name. The creating user SHALL be recorded as that company's `ADMIN`. The company SHALL start with no plan assigned; plan selection and the company's commercial profile (Razão Social, Nome Fantasia, CNPJ, address, logo) are handled separately (see `company-onboarding`, `company-plan-catalog`).

#### Scenario: Company created successfully
- **WHEN** an authenticated user with no company submits a company name
- **THEN** `pantheon-service` persists a new `Company` (identified by the given name) with no plan and no profile fields set, and creates a `CompanyMembership` linking the creator to the company with role `ADMIN`

#### Scenario: User with an existing company is not offered creation again
- **WHEN** an authenticated user who already administers or belongs to a company attempts to create another company
- **THEN** `pantheon-service` still allows it (a user may administer more than one company), recording the new company independently with its own plan and profile state

### Requirement: List administered and joined projects
`pantheon-service` SHALL allow an authenticated user to retrieve the list of companies they belong to, along with their role on each and each company's onboarding status.

#### Scenario: User lists their companies
- **WHEN** an authenticated user requests their list of companies
- **THEN** `pantheon-service` returns every company for which the user has a `CompanyMembership`, including their role (`ADMIN` or `MEMBER`) and onboarding status on each

## REMOVED Requirements

### Requirement: Trial period
**Reason**: A time-boxed trial no longer gates usability — a plan must be selected immediately after company creation (see `company-onboarding`), and plan selection carries no charge, so there is nothing left for a trial window to protect against.
**Migration**: See `company-onboarding`'s "Mandatory plan selection" requirement.

### Requirement: Plan confirmation
**Reason**: Plan choice is now driven by a registered plan catalog and is made mandatory during onboarding rather than reactively after a trial or prior plan expires.
**Migration**: See `company-plan-catalog`'s "Plan selection" and "Plan change" requirements, and `company-onboarding`'s "Mandatory plan selection".

### Requirement: Onboarding status
**Reason**: Onboarding status is redefined around the new plan-then-profile sequence (`PLAN_PENDING`/`PROFILE_PENDING`/`COMPLETE`) rather than trial/plan-expiry state.
**Migration**: See `company-onboarding`'s "Onboarding status computation" requirement.

### Requirement: Project membership management
**Reason**: Company-wide membership with a construction function is replaced by two distinct concepts: company staff membership (no construction function, using the existing invitation lifecycle) and construction-site-scoped team membership.
**Migration**: See `team-invitations` for company staff invitations and `obra-team-management` for site team membership.
