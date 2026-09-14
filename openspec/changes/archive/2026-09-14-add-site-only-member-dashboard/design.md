## Context

The router guard (`pantheon-web/src/router/index.ts`) already distinguishes `hasCompany` (any `CompanyMembership`, admin or staff) from a pure site-team member (`SiteMembership` only — client, architect, engineer, site foreman, service provider). Today the `!hasCompany` branch always redirects to `siteIds[0]` with no way to see or reach the rest. `OnboardingStatus.siteIds` already exists (bare UUIDs, no site detail). Nothing today resolves a company's name/logo for a user who isn't that company's staff — `CompanyService.get`/`getLogo` both require `CompanyMembership`, which a site-only member by definition doesn't have.

## Goals / Non-Goals

**Goals:**
- A site-only user with 2+ obras sees all of them, across whichever companies they belong to, and can navigate between them.
- A site-only user with exactly 1 obra keeps the current direct-entry behavior — no extra click.
- The admin/staff dashboard is untouched — this is a parallel rendering path gated on `!hasCompany`, not a rewrite.
- "Editar dados do cadastro" actually works — closing the gap where the backend existed but nothing called it.

**Non-Goals:**
- No handling for a "mixed" user who has *both* a `CompanyMembership` somewhere *and* a `SiteMembership` elsewhere — the router guard has only ever branched on `hasCompany`, and extending that to merge two fundamentally different dashboard shapes is a separate, larger problem than what was asked here.
- No change to `activeCompany()`'s tie-break when a user administers multiple companies — out of scope, pre-existing behavior.
- No editing of company data from this reduced menu — by design, only "Sair" and the user's own profile.

## Decisions

### 1. `GET /api/construction-sites/mine`, not a bigger rework of the existing company-scoped endpoint
`ConstructionSiteService.list(companyId, userId)` requires `CompanyMembership` and returns every site *in that company* — wrong shape and wrong auth for this case entirely. A new `listMine(actingUserId)` builds from `SiteMembershipRepository.findByUserId` (already exists) → active site ids → `ConstructionSiteRepository.findAllById` (inherited, unused until now) → batch-resolve owning companies' names via `CompanyRepository.findAllById` (also inherited, unused until now). Returned as a new `MySiteResponse` carrying `companyName` inline, so the frontend never needs a separate per-company fetch it isn't authorized to make anyway.

### 2. A new site-scoped logo endpoint, not a relaxed company-membership check
`GET /api/companies/{id}/logo` requires `CompanyMembership` — correct for the admin dashboard, wrong for this case (relaxing it would let any company's site-only member fetch that company's logo even without ever being told the company's id, a minor but needless widening of an existing, unrelated endpoint's contract). Instead, `GET /api/construction-sites/{id}/company-logo` resolves the logo through the *site*, gated by `SiteAccessService.requireAccess` (which already correctly authorizes both company staff and `SiteMembership`) — mirrors the existing site-photo-content endpoint's pattern of gating by site access rather than company access.

### 3. Router guard: redirect only when there's exactly one obra
```
!hasCompany && siteIds.length === 0  → company-new (unchanged)
!hasCompany && siteIds.length === 1  → straight into that site-detail (unchanged)
!hasCompany && siteIds.length > 1    → dashboard (new — was siteIds[0])
```
This is a one-line change in the existing guard, not new branching logic — `to.name === 'dashboard'` already special-cased the redirect; removing the `siteIds.length > 1` case from that special-case lets it fall through to `return true`, i.e. render the dashboard normally.

### 4. `DashboardView.vue` branches internally rather than becoming two components
`hasCompany` is already known at the top of the component (same `useCompanyOnboarding` call already imported). Header markup, the profile-menu template, and the data-loading function branch on it; the obra-card grid markup is shared (same visual card), with an extra `<SiteCompanyBadge>` rendered only in the site-only path. This keeps one file instead of duplicating the whole page, while every admin/staff-facing branch is provably unchanged (still gated behind the pre-existing `hasCompany` truthy path).

### 5. "Editar dados do cadastro" fulfills the already-specified, never-implemented profile requirement
`pantheon-service`'s spec already documents "User profile registration" (create + update via upsert) — the entity, repository, and `UserProfileService.upsert` exist, but no controller, no `GET`, and no frontend ever called any of it. This change adds exactly the missing pieces (a `get` method, a controller, a view) rather than designing a new feature — the data model and validation rules (`cnpjCpf`, `legalName`, `address`, `postalCode`, all required once a profile exists) were already decided when that entity was built.

## Risks / Trade-offs

- **[Trade-off] `MySiteResponse` denormalizes `companyName` rather than the frontend composing it from a separate company fetch.** → Accepted: the alternative would need a *third* new endpoint (batch company name lookup, site-access-gated) for no real benefit — denormalizing one string is simpler and avoids an N+1 on the frontend.
- **[Risk] A site-only user in a very large number of obras gets an unpaginated list.** → Accepted at current scale (matches how the admin dashboard's obra grid is already unpaginated); revisit only if it becomes a real problem.
