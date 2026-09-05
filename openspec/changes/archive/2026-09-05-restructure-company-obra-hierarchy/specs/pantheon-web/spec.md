## REMOVED Requirements

### Requirement: Post-login onboarding popup
**Reason**: Replaced by the mandatory, ordered signup flow (company creation → plan selection → company profile) — a fresh signup no longer chooses between "create" or "join"; joining a company/site now happens exclusively via invitation acceptance.
**Migration**: See `company-onboarding`'s "Company creation is name-only" and "Mandatory plan selection" requirements.

### Requirement: No-project message on join
**Reason**: There is no more "join a project" choice on first login to react to; an invited user reaches their target directly via the invitation link.
**Migration**: See `team-invitations` and `obra-team-management` for the invitation-acceptance flow.

### Requirement: Project registration view
**Reason**: Replaced by a name-only company creation step followed by a separate, mandatory company profile form (Razão Social, Nome Fantasia, CNPJ, endereço, logo) that no longer bundles the user's own CNPJ/CPF profile into project creation.
**Migration**: See `company-onboarding`'s "Company creation is name-only" and "Company profile completion" requirements.

### Requirement: Plan selection view
**Reason**: Plan selection is no longer a reactive screen shown on trial/plan expiry — it is a mandatory modal shown immediately after company creation, sourced from the registered plan catalog.
**Migration**: See `company-onboarding`'s "Mandatory plan selection" and `company-plan-catalog`'s "Plan selection" requirements.

### Requirement: Post-login routing guard
**Reason**: Redefined around the new onboarding sequence (company creation → plan → profile → dashboard) instead of the old active-project check.
**Migration**: See `company-onboarding`'s "Post-login routing guard order" requirement.
