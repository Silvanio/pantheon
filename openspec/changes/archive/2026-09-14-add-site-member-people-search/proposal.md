## Why

Adding a member to an obra's team today means retyping that person's data from scratch every time, even when they're already known to the company (staff on another obra, a previously invited client, a service provider added before). There's also no name field collected for the email-invite paths (CLIENT/ARCHITECT/ENGINEER/SITE_FOREMAN) and no phone field anywhere, and neither CPF nor email is format-validated server-side.

## What Changes

- Add a company-scoped people-search endpoint (`GET /api/sites/{siteId}/people/search?q=...`) that looks up people already known to the company — by CPF or email prefix — across its construction sites' team members and company staff.
- `SiteTeamPanel.vue`'s add-member form gains autocomplete on both the CPF and email fields, backed by that search; picking a suggestion pre-fills the form and relabels the submit button to "Enviar convite" (same underlying add/invite call as today — the person is simply already known, so nothing else needs retyping). Typing a genuinely new person's details and clicking "Adicionar" behaves exactly as it does today.
- Add a `name` field to the form, required for every function (previously only the accountless service-provider path had one); a `phone` field, optional for every function, same as CPF already is.
- Generalize `SiteMembership.clientCpf` (CLIENT-only) into `SiteMembership.cpf` (any function) and add `SiteMembership.phone`, both optional; a new migration renames/adds the columns.
- When inviting someone who doesn't have an account yet, the submitted name seeds their pre-registration account's display name (instead of only the email-derived default); an existing account's own name is never overwritten.
- Backend validation: email format (`@Email`) plus "required except for an accountless service provider" enforced explicitly; CPF format **and** check-digit validation (standard Brazilian algorithm) when a CPF is supplied, rejecting both malformed and malformed-but-well-formed-looking (e.g. all-same-digit) CPFs.

## Capabilities

### New Capabilities
(none — extends `obra-team-management`)

### Modified Capabilities
- `obra-team-management`: adds the people-search/autocomplete behavior, the new required `name` and optional `phone` fields, CPF availability beyond CLIENT, and backend CPF/email validation.

## Impact

- Affected code: `pantheon-service` — new migration, `SiteMembership` entity, `AddSiteMemberRequest`/`SiteMemberResponse` DTOs, new `PersonSearchController`/`PersonSearchService` (or equivalent), `SiteMembershipService`, `MembershipInvitationIssuer` (new overload, backward compatible), a new `CpfValidator` utility, a new `InvalidCpfException`.
- Affected code: `pantheon-web` — `SiteTeamPanel.vue`, `useSiteMembers.ts` (new search call + updated types), `pt-BR.json`.
- **Security-relevant decision**: the search is scoped strictly to the acting company's own known people (its staff plus every construction site's team, account-holding or not) — never a cross-company/platform-wide lookup, since CPF and contact details are sensitive and must not leak between tenants. This is the interpretation used throughout the design below; flag it if a broader search was actually intended.
- No removal of the existing accountless service-provider flow: it remains the one path where email is not required (its defining trait per `SiteMembership`'s existing Javadoc).
