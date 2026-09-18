## 1. Backend: access level and defaults

- [x] 1.1 Add `VIEW_AND_APPROVE` to `AccessLevel` (`pantheon-service/src/main/java/com/pantheon/service/entity/AccessLevel.java`), updating its doc comment to explain it's meaningful only for `PURCHASE_REQUEST`.
- [x] 1.2 Update `SitePermissionService`'s `DEFAULTS` map: `CLIENT`'s `PURCHASE_REQUEST` default becomes `VIEW_AND_APPROVE` (keep `ORCAMENTO_MANAGE` at `VIEW`). Update the class-level and `resolve`-level doc comments that currently claim `PURCHASE_REQUEST` only ever uses `MANAGE`/presence.
- [x] 1.3 Confirm `canManage`/`requireManage` treat `VIEW_AND_APPROVE` as not-manage (already true since they check `== AccessLevel.MANAGE`) — no code change expected, just confirm with a test.

## 2. Backend: approval-step authorization

- [x] 2.1 Rewrite `PurchaseRequestService.requireStepAuthority` to resolve the acting user's real, active `SiteMembership` on that site directly via `siteMembershipRepository.findByConstructionSiteIdAndUserId(...)` (independent of `SiteAccessContext.companyStaff()`), per design.md decision 1.
- [x] 2.2 When an active `SiteMembership` is found, require its function to equal `step.getApproverFunction()` with no staff exception; on match, additionally require the resolved `PURCHASE_REQUEST` access level (via `SitePermissionService.resolve`) to be `MANAGE` or `VIEW_AND_APPROVE`, else throw `NotCurrentApprovalStepException`.
- [x] 2.3 When no active `SiteMembership` exists on that site, keep the `companyStaff` bypass (unrestricted approval authority) as the sole fallback.
- [x] 2.4 Update `approveStep`/`rejectStep` to derive `decidedBySiteMembershipId` from whichever `SiteMembership` `requireStepAuthority` resolved (present whenever one was found, `null` only for the pure-staff-no-membership fallback) instead of `access.companyStaff() ? null : ...`.

## 3. Backend: dynamic visibility for VIEW_AND_APPROVE

- [x] 3.1 Add `PurchaseRequestApprovalRepository.existsByPurchaseRequestIdAndCycleNumberAndApproverFunctionAndStatusNot(UUID, int, ConstructionFunction, PurchaseRequestApprovalStatus)`. (Corrected from the original plain "exists for this function, any status" query — see 3.2's note.)
- [x] 3.2 Add a `PurchaseRequestService` helper (`isVisibleToViewAndApprove(PurchaseRequest, SiteMembership)`) implementing design.md decision 4: visible if `CONCLUIDO`; else if the member's function already decided (non-`PENDING`) a step this cycle (query above); else if the member's function matches the cycle's currently-actionable step (`findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(..., PENDING)`). **Post-implementation correction**: the original plan checked "does any approval row for my function exist in this cycle, in any status" — but `submitForApproval` creates every level's row `PENDING` up front, so that check was true from the moment of submission for every approver, letting a later-step approver (e.g. `CLIENT` behind `ENGINEER`) see and be shown approve/reject actions before it was actually their turn. Fixed to key off the specific actionable step instead. Reported directly as a regression after the first implementation shipped.
- [x] 3.3 Apply the helper in `list()`: when the resolved `PURCHASE_REQUEST` access level is `VIEW_AND_APPROVE`, filter the fetched page's content through it (member resolved from the site membership on `SiteAccessContext`, falling back to a direct repository lookup the same way as 2.1 if needed for a staff member who is also a site member).
- [x] 3.4 Apply the helper in `get()`, `getComparison()`, `listInvoices()`, `getInvoiceContent()`: when access level is `VIEW_AND_APPROVE` and the helper returns false, throw `PurchaseRequestNotFoundException` (not a 403) so an irrelevant PR looks nonexistent.
- [x] 3.5 Leave `VIEW` and `MANAGE` behavior unchanged (full, unfiltered visibility).

## 4. Backend: tests

- [x] 4.1 Rewrite `companyStaffCanAlwaysActOnApprovalStep` into two tests: staff with no `SiteMembership` on the site can still approve any step; staff who also has an active `SiteMembership` with a non-matching function is blocked (`NotCurrentApprovalStepException`).
- [x] 4.2 Add a test: staff who also has an active `SiteMembership` with a matching function can approve, and the approval is attributed to that `SiteMembership` (not `null`).
- [x] 4.3 Add a test: a member whose function matches the pending step but whose `PURCHASE_REQUEST` access is `VIEW` is blocked from approving.
- [x] 4.4 Add `PurchaseRequestServiceTest` coverage for `list()`/`get()` dynamic visibility: a `VIEW_AND_APPROVE` member can't see a PR before their function's step exists, can see it once their step is `PENDING`, can still see it after deciding, and can always see a `CONCLUIDO` PR regardless of involvement.
- [x] 4.5 Add/adjust `SitePermissionServiceTest` coverage for the `CLIENT` default change and `VIEW_AND_APPROVE` resolution/override round-trip.
- [x] 4.6 Run `./mvnw test` from `pantheon-service/` and fix any other call sites broken by the constructor/behavior changes.

## 5. Frontend: permission configuration UI

- [x] 5.1 Add `'VIEW_AND_APPROVE'` to the `AccessLevel` union type in `pantheon-web/src/composables/useSitePermissions.ts`.
- [x] 5.2 Add a pt-BR label for it (e.g. "Apenas ver e aprovar") and wire it into the access-level `<select>` in `SitePermissionsPanel.vue`, restricted to the `PURCHASE_REQUEST` row (other capabilities keep only Ver/Gerenciar/Oculto).

## 6. Frontend: Pedido de Compra action visibility

- [x] 6.1 In `PurchaseRequestPanel.vue`, hide/disable the "Novo pedido" create button when the viewer's resolved `PURCHASE_REQUEST` access is not `MANAGE`.
- [x] 6.2 In `PurchaseRequestDetailView.vue`, hide the "Enviar para aprovação" action unless access is `MANAGE`, and hide the approve/reject actions unless access is `MANAGE` or `VIEW_AND_APPROVE` **and** the viewer's site-membership function matches the current pending step (mirrors the backend check, purely for UX — the backend remains the source of truth). **Revision**: the first pass only gated on access level (no function match), which shipped a second reported regression — an `ENGINEER` with `MANAGE` still saw active approve/reject buttons once the pending step moved on to the `CLIENT`'s turn. There was no frontend source for "my own function on this site" (`useCompanyOnboarding`/`useSitePermissions` only expose capability access levels), so added one: new backend endpoint `GET /api/sites/{siteId}/members/mine` (`SiteMembershipController.mine`, backed by `SiteMembershipService.findMyFunction`) returning the caller's real `SiteMembership` function independent of company-staff status (mirrors `PurchaseRequestService`'s own `resolveSiteMembership` fix from section 2 — `SiteAccessContext.function()` is null for staff even when they hold a matching membership). Frontend: `useSiteMembers().getMyFunction(siteId)`, consumed in `PurchaseRequestDetailView.vue` as `myFunction`. `canActOnApproval` now requires `myFunction === currentPendingApproval.approverFunction` (plus the access-level check) whenever the viewer holds any `SiteMembership` on the site at all; only a company-staff viewer with **no** `SiteMembership` there falls back to the pure access-level check (mirroring the backend's staff-with-no-membership bypass).
- [x] 6.3 Add any new pt-BR strings needed for the above. (None needed — no new copy, only visibility logic changed.)
- [x] 6.4 Run `npm run build` from `pantheon-web/` and fix any type errors.

## 7. Manual verification

- [x] 7.1 Walk through the reported scenario: a user who is both company staff and a `CLIENT` `SiteMembership` on a 2-step (`ENGINEER` → `CLIENT`) Pedido de Compra cannot approve the `ENGINEER` step, cannot see the Pedido de Compra while the `ENGINEER` step is pending, and can approve once the `CLIENT` step becomes pending. Verified via the automated backend test suite (`companyStaffWithNonMatchingSiteMembershipIsBlockedDespiteBeingStaff`, `companyStaffWithMatchingSiteMembershipApprovesAsThatMembership`, `getRejectsViewAndApproveMemberBeforeTheirStepExists`, `getAllowsViewAndApproveMemberOnceTheirStepIsPending`) — no login credentials were available in this session for a live browser walkthrough.

## 8. Frontend follow-up: approve/reject must match the current step's function, not just access level

- [x] 8.1 Add `SiteMembershipService.findMyFunction(siteId, userId)` (`pantheon-service`): resolves the caller's real, active `SiteMembership` function on a site independent of company-staff status (same fix shape as `PurchaseRequestService.resolveSiteMembership`). New DTO `SiteMemberFunctionResponse`.
- [x] 8.2 Add `GET /api/sites/{siteId}/members/mine` (`SiteMembershipController.mine`) exposing it — open to any active member, not gated by `TEAM_MANAGE` (same "any member may learn their own state" pattern as `SitePermissionController.mine`).
- [x] 8.3 Add `useSiteMembers().getMyFunction(siteId)` (`pantheon-web`) and consume it in `PurchaseRequestDetailView.vue` as `myFunction`.
- [x] 8.4 Rewrite `canActOnApproval`: when the viewer holds any `SiteMembership` on the site, require `myFunction === currentPendingApproval.approverFunction` (in addition to the existing access-level check); only a viewer with **no** `SiteMembership` on the site falls back to the pure access-level check.
- [x] 8.5 Backend tests: `SiteMembershipServiceTest.findMyFunctionReturnsTheCallersOwnFunction`, `...EvenWhenTheyAreAlsoCompanyStaff`, `...ReturnsNullForStaffWithNoSiteMembership`.
- [x] 8.6 Run `./mvnw test` and `npm run build` again; fix any breakage.

## 9. Conclude requires approve-capable access, not strict manage

- [x] 9.1 Add `SitePermissionService.canApprove`/`requireApprove` (`MANAGE` or `VIEW_AND_APPROVE`); refactor `PurchaseRequestService.requireStepAuthority` to call it instead of duplicating the level check inline.
- [x] 9.2 Switch `PurchaseRequestService.conclude()` from `requireManage` to `requireApprove`.
- [x] 9.3 Frontend: `PurchaseRequestDetailView.vue`'s `canConclude` now also requires `canApprove` (`MANAGE` or `VIEW_AND_APPROVE`), reusing the same computed the approve/reject buttons already use.
- [x] 9.4 Backend tests: `concludeRejectsMemberWithoutApproveAccess`, `concludeChecksApproveAccessNotStrictManage`; `SitePermissionServiceTest.requireApprovePassesForViewAndApprove`, `requireApproveThrowsForViewOnly`. Updated existing `approveStep`/`rejectStep` tests to stub `canApprove` instead of the now-bypassed `resolve` call.
- [x] 9.5 Run `./mvnw test` and `npm run build` again; fix any breakage.
