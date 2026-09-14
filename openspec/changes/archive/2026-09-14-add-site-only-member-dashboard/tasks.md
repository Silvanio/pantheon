## 1. Backend: my-sites endpoint

- [x] 1.1 Add `MySiteResponse` DTO (`id`, `companyId`, `companyName`, `name`, `address`, `status`, `startDate`, `expectedEndDate`, `photoObjectKey`).
- [x] 1.2 `ConstructionSiteService.listMine(actingUserId)`: active `SiteMembership`s for the user → distinct site ids → `siteRepository.findAllById` → distinct company ids → `companyRepository.findAllById` → assemble responses.
- [x] 1.3 Add `GET /api/construction-sites/mine` to `ConstructionSiteController`, calling `listMine`.

## 2. Backend: site-scoped company logo

- [x] 2.1 Add `ConstructionSiteService.getCompanyLogoObjectKey(siteId, actingUserId)`, gated by `SiteAccessService.requireAccess` (not company membership) via the existing `get(...)` method.
- [x] 2.2 Add `GET /api/construction-sites/{id}/company-logo` returning the logo bytes (mirrors the existing `CompanyController.getLogo`'s storage call).

## 3. Backend: user profile GET

- [x] 3.1 `UserProfileService`: add a `get(userId)` returning `Optional<UserProfile>`.
- [x] 3.2 Add `UserProfileResponse` DTO and `UpdateUserProfileRequest` DTO (`cnpjCpf`, `legalName`, `address`, `postalCode`, all required).
- [x] 3.3 Add `UserProfileController`: `GET /api/users/me/profile` (204 when none exists), `PUT /api/users/me/profile` (upsert).

## 4. Router guard

- [x] 4.1 `router/index.ts`: only redirect straight into `siteIds[0]` when `siteIds.length === 1`; otherwise (more than one) let navigation to `dashboard` through instead of forcing a redirect.

## 5. Frontend: data layer

- [x] 5.1 New `useMySites.ts`: `MySite` type, `listMine()`, `getSiteCompanyLogoBlob(siteId)`.
- [x] 5.2 New `useUserProfile.ts`: `UserProfile` type, `getMyProfile()`, `updateMyProfile(data)`.

## 6. Frontend: dashboard changes (admin/staff path untouched)

- [x] 6.1 `DashboardView.vue`: `loadSites()` calls `listMine()` instead of bailing out when `!companyId`; `sites` typed to optionally carry `companyName`.
- [x] 6.2 Hide the "Nova obra" button/form for the site-only case (`v-if="companyId"`) — they can't create a site without a company.
- [x] 6.3 Add "Editar dados do cadastro" to the profile menu, shown only when `!companyId` (`v-else` alongside the existing `v-if="companyId"` company-settings link), linking to the new profile route.
- [x] 6.4 Add `SiteCompanyBadge.vue` (small logo + company name pill) and render it on each obra card when `site.companyName` is present.

## 7. Frontend: user profile view

- [x] 7.1 New `UserProfileView.vue`: loads existing profile on mount (blank if none), form for CNPJ/CPF, legal name, address, CEP, submit calls `updateMyProfile`, back link to dashboard.
- [x] 7.2 New route `/profile` (name `user-profile`) in `router/index.ts`.
- [x] 7.3 `pt-BR.json`: new locale keys for the profile view and the new menu item.

## 8. Verification

- [x] 8.1 Backend: unit tests for `ConstructionSiteService.listMine` (cross-company aggregation, only active memberships, company name resolution), `getCompanyLogoObjectKey`, and `UserProfileService.get`. Full suite: 133/133 passing.
- [x] 8.2 Frontend: `npm run build` green.
- [ ] 8.3 Manual check left for the user: single-obra site-only user still enters directly; multi-obra site-only user sees the dashboard with company badges and the reduced menu; admin dashboard is visually/functionally unchanged; profile edit page creates then updates correctly. **Not run in this session** — the assistant could not log in to verify live.
