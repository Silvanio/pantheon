## Why

A user with no `CompanyMembership` — only `SiteMembership` access to one or more obras, possibly across different companies (a client, an outside architect/engineer, a service provider) — is currently dumped straight into whichever obra the router guard happens to pick first (`siteIds[0]`, an arbitrary order). If they have more than one obra, the rest are invisible: there is no list, no way to switch, and clicking "Voltar" just bounces them back to the same forced redirect. This is a real gap, not a preference — confirmed nothing today lists a site-only user's obras across companies, shows which company each belongs to, or lets such a user edit their own registration data (the backend for that exists but was never wired to any endpoint or UI).

## What Changes

- Router guard: a site-only user (`hasCompany === false`) with exactly one obra keeps going straight into it (unchanged). With more than one, they now land on the dashboard instead of an arbitrary first obra.
- New dashboard variant, shown only for this site-only, multi-obra case — **the existing company-scoped dashboard for admins/staff is completely unchanged**:
  - Header shows the generic Pantheon brand (icon + name), not any one company's branding.
  - The profile menu is reduced to exactly two items: "Sair" and "Editar dados do cadastro" (no "Alterar plano", no "Editar dados da empresa" — those are company-staff actions that don't apply here).
  - The obra grid is sourced from every company's obra the user has active `SiteMembership` access to (new backend endpoint), and each card carries a small badge with that obra's company's logo and name, since obras can now span multiple companies.
- "Editar dados do cadastro" is a genuinely new page: the backend entity/service for a user's own registration profile (CNPJ/CPF, legal name, address, CEP) already existed but was never wired to any controller or frontend — this change adds the missing GET/PUT endpoint and a simple edit form.

## Capabilities

### New Capabilities
(none — extends `company-onboarding`)

### Modified Capabilities
- `company-onboarding`: the routing guard's site-only-user behavior is refined (single vs. multi-obra), the dashboard gains a distinct rendering for this case, obra cards gain a company badge when the dashboard spans multiple companies, and the header profile menu gains the reduced two-item variant.
- `pantheon-service`: the existing (but unimplemented) "User profile registration" requirement gains its missing read path (`GET`), completing what was already specified.

## Impact

- Affected code: `pantheon-service` — new `GET /api/construction-sites/mine` (+ `MySiteResponse` DTO, `ConstructionSiteService.listMine`), new `GET /api/construction-sites/{id}/company-logo` (site-access-gated, not company-membership-gated, so a site-only member can fetch the badge logo), new `UserProfileController` (`GET`/`PUT /api/users/me/profile`), `UserProfileService` gains a `get` method.
- Affected code: `pantheon-web` — `router/index.ts` guard, `DashboardView.vue` (branches on `hasCompany`), new `SiteCompanyBadge.vue`, new `useMySites.ts`, new `useUserProfile.ts`, new `UserProfileView.vue` + route.
- No schema changes (the `user_profile` table already exists, unused until now).
- No change whatsoever to the admin/staff dashboard experience — every change here is gated on `!hasCompany`.
