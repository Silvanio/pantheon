## Context

`SiteAccessService.resolve` decides `companyStaff` vs. site member by checking `CompanyMembership` first: if the acting user has *any* active `CompanyMembership` on the site's owning `Company`, it returns `new SiteAccessContext(true, null)` — `siteMembership` is `null` even if that same user also has an active `SiteMembership` on that specific site. `PurchaseRequestService.requireStepAuthority` then does `access.companyStaff() || access.function() == step.getApproverFunction()`, so a company-staff user always passes, regardless of their real site role. This is deliberate, tested behavior (`companyStaffCanAlwaysActOnApprovalStep`), but it is wrong whenever the same user also plays a specific approval role on that site — which is exactly the reported case (a company owner registered as `CLIENT` on one of their own sites).

Separately, `PURCHASE_REQUEST` visibility (`list`/`get`/`getComparison`/`listInvoices`/`getInvoiceContent`) only checks `requireVisible` (rejects `HIDDEN`), so any non-`HIDDEN` member sees every Pedido de Compra on the site unconditionally. There is no notion of "this doesn't concern you yet."

## Goals / Non-Goals

**Goals:**
- Approval-step authority always respects a real `SiteMembership`'s function when one exists on that site, even for company staff.
- Preserve an approval "escape hatch" for company staff who have no `SiteMembership` at all on the site (pure admins), per explicit product decision.
- Introduce `AccessLevel.VIEW_AND_APPROVE` for `PURCHASE_REQUEST`, with dynamic, per-member visibility.
- Keep `VIEW` and `MANAGE` semantics for every other capability unchanged.

**Non-Goals:**
- No change to `ORCAMENTO_MANAGE` semantics or to approval levels configuration (`SitePurchaseRequestApprovalLevel`).
- No retroactive visibility across approval cycles — dynamic visibility for `VIEW_AND_APPROVE` is scoped to the Pedido de Compra's `currentApprovalCycle` (matches the existing cycle-scoped pattern already used by `PurchaseRequestPdfService`).
- No change to how `Material` conclusion or delivery tracking works.

## Decisions

### 1. Resolve the real `SiteMembership` independently of `companyStaff`

`requireStepAuthority` stops trusting `SiteAccessContext.siteMembership()` (which is `null` for staff) and instead always looks up `siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, userId)` (filtered to `isActive()`), regardless of `companyStaff`. Then:
- If an active `SiteMembership` exists: its function **must** equal `step.getApproverFunction()`, with no exception for staff. On match, it returns a `SiteAccessContext(companyStaff, thatMembership)` so `decidedBySiteMembershipId` correctly attributes the decision to that person (not `null`/"Equipe da empresa").
- If no active `SiteMembership` exists on that site: fall back to the current `companyStaff` bypass (unrestricted, `decidedBySiteMembershipId = null`).

Alternative considered: keep `SiteAccessContext.siteMembership()` as-is and have `SiteAccessService.resolve` populate both `companyStaff` and the real membership simultaneously. Rejected for this change — it would touch `SiteAccessContext`'s contract and every other caller (`SitePermissionService.resolve`, etc.) that currently assumes `companyStaff ⇒ siteMembership == null`, a much larger blast radius than doing the extra lookup locally in `PurchaseRequestService`.

### 2. Gate approval on the `PURCHASE_REQUEST` access level too

`requireStepAuthority` additionally resolves the member's `PURCHASE_REQUEST` `AccessLevel` via `SitePermissionService` and rejects (same `NotCurrentApprovalStepException`) unless it is `MANAGE` or `VIEW_AND_APPROVE`. This makes `VIEW` genuinely read-only even when a `VIEW` member's function happens to match a pending step — closing the second gap called out in the bug report ("Ver... nunca pode aprovar nada").

### 3. `VIEW_AND_APPROVE` as a shared `AccessLevel`, `PURCHASE_REQUEST`-only in practice

Rather than a capability-specific enum, `VIEW_AND_APPROVE` is added to the existing `AccessLevel` enum (like `VIEW`/`MANAGE`/`HIDDEN`), documented as meaningful only for `PURCHASE_REQUEST` — consistent with how the enum's doc comment already calls out `PURCHASE_REQUEST`/`ORCAMENTO_MANAGE` as special-cased capabilities. `SitePermissionService.canManage`/`requireManage` treat `VIEW_AND_APPROVE` as *not* manage (same as `VIEW`), so create/submit/conclude/delete continue to require `MANAGE`.

### 4. Dynamic visibility as a reusable predicate

A new `PurchaseRequestService` helper, `isVisibleToViewAndApprove(PurchaseRequest, SiteMembership)`, returns `true` when the PR is `CONCLUIDO`; otherwise it checks two things, in order: (a) whether their function already decided (`APPROVED`/`REJECTED`) a step in the current cycle — "their turn already happened" — via a new repository query, `existsByPurchaseRequestIdAndCycleNumberAndApproverFunctionAndStatusNot(..., PENDING)`; and if not, (b) whether their function matches the cycle's *currently actionable* step, i.e. the lowest-order `PENDING` step, reusing the same `findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc` query `requirePendingStep`/`approveStep` already use.

**Correction from the original version of this decision**: `submitForApproval` creates *every* configured level's `PurchaseRequestApproval` row as `PENDING` up front, in the same transaction, not incrementally as each prior step completes (only the *notification* is sent step-by-step). A first draft of this predicate checked "does any row for my function exist in this cycle, in any status" — which is true from the moment of submission for every level, including ones whose turn hasn't come yet. That let a second-step approver see (and be shown UI to act on) a Pedido de Compra while the first step was still outstanding — reported directly as a regression. The fix is to check the specific step that's actionable *right now* (or one they already decided), not merely "a row for my function exists somewhere in this cycle."

`list()` filters the page's content through this predicate post-fetch when the resolved access level is `VIEW_AND_APPROVE` (simplest correct approach; the purchase-request table is site-scoped and paginated in modest sizes, so an in-memory filter after the specification query is acceptable — no need for a new JPA specification join). `get()`, `getComparison()`, `listInvoices()`, `getInvoiceContent()` throw the existing `PurchaseRequestNotFoundException` when the predicate fails for a `VIEW_AND_APPROVE` member, so an irrelevant PR behaves as if it doesn't exist rather than leaking a 403 that confirms its existence.

Alternative considered: filter via a JPA `Specification` join against `PurchaseRequestApproval` so pagination counts stay accurate under filtering. Rejected for now — pagination undercounting for `VIEW_AND_APPROVE` members (a niche, low-volume role) is an acceptable trade-off against the complexity of a correlated-subquery specification; can revisit if it proves confusing in practice.

### 5. `CLIENT` default becomes `VIEW_AND_APPROVE`

Matches the client's real-world role (approver of their own step, not a general manager). `SERVICE_PROVIDER` and `SITE_FOREMAN` keep `VIEW` (real read-only now, a strict improvement over today's inconsistently-enforced default).

### 6. A dedicated "my function" endpoint for the frontend's approve/reject gating

The first pass at hiding the approve/reject buttons on the frontend checked only the viewer's resolved `PURCHASE_REQUEST` access level (`MANAGE`/`VIEW_AND_APPROVE`), not whether the *currently pending step* actually matched their own function — because no existing frontend call exposed "what is my function on this site" (`useSitePermissions().getMyPermissions` only returns capability access levels, which are role-derived but not the role itself). This shipped a second regression: an `ENGINEER` with `MANAGE` access kept seeing active approve/reject buttons after the pending step moved on to the `CLIENT`.

Fix: a small, purpose-built endpoint, `GET /api/sites/{siteId}/members/mine` (`SiteMembershipController.mine` → `SiteMembershipService.findMyFunction`), returning the caller's real, active `SiteMembership` function on that site — `null` if they hold none there. Like `PurchaseRequestService`'s own `resolveSiteMembership` (decision 1), this cannot reuse `SiteAccessContext.function()`: that comes back `null` for company staff even when they *do* hold a matching `SiteMembership`, which is exactly the case this whole change is about. The frontend (`useSiteMembers().getMyFunction`, consumed by `PurchaseRequestDetailView.vue`) now requires `myFunction === currentPendingApproval.approverFunction` before showing approve/reject, falling back to the pure access-level check only when the viewer holds no `SiteMembership` on the site at all — mirroring the backend's own staff-with-no-membership bypass exactly.

Alternative considered: extend the existing `/api/sites/{siteId}/permissions/mine` response to also carry the function, avoiding a new endpoint. Rejected — that endpoint's response shape (`Record<PermissionCapability, AccessLevel>`) is consumed directly (spread across capability keys) by `SiteDetailView.vue` for several unrelated tabs; reshaping it to `{ capabilities, function }` would force updating every call site for a concern (approval-step authorship) that's specific to Pedidos de Compra. A dedicated endpoint keeps the blast radius to exactly the two files that need it.

### 7. "Conclude" shares the same authorization as acting on a step

`conclude()` originally called the same `requireManage` used by `create`/`submit`/`delete` — strict `MANAGE` only. That's wrong for this feature: a `VIEW_AND_APPROVE` member (the client) can legitimately be the one who approves the *final* step of a Pedido de Compra, and per the reported feedback, whoever can approve should also be able to conclude it — not just full managers. Added `SitePermissionService.canApprove`/`requireApprove` (`MANAGE` or `VIEW_AND_APPROVE`) and switched `conclude()` to it. `requireStepAuthority` was also refactored to call `canApprove` instead of duplicating the same `level == MANAGE || level == VIEW_AND_APPROVE` check inline. Frontend: `PurchaseRequestDetailView.vue`'s `canConclude` now additionally requires `canApprove` (mirroring the same access-level computed already used for the approve/reject buttons) — no function-matching needed here, since by the time a Pedido de Compra reaches `CONFERIDO` every step has already been individually function-checked; concluding is a final, role-level action, not tied to a specific pending step.

## Risks / Trade-offs

- [Pagination totals can overcount for `VIEW_AND_APPROVE` members (a page may render fewer rows than `totalElements` claims)] → Acceptable for this narrow role; revisit with a join-based specification if it causes visible UI glitches.
- [Narrowing the staff bypass is a **BREAKING** authorization change] → Intentional per the reported bug; covered by rewritten tests for both branches (staff-with-membership blocked, staff-without-membership still allowed).
- [Existing sites where `CLIENT` was relying on `VIEW`'s old "denied" behavior might now see more via real `VIEW`, or the default flips them to `VIEW_AND_APPROVE`] → Only affects sites with no explicit `CLIENT` function-level override already set; explicit overrides made through the permissions UI are preserved as-is.

## Migration Plan

No schema migration required — `AccessLevel` is a Java enum persisted as a string in `SitePermissionOverride`; adding a new constant is additive. No data backfill needed. Deploy backend and frontend together (frontend needs `VIEW_AND_APPROVE` in its access-level union type/labels before the backend can meaningfully return it from the permissions endpoint).
