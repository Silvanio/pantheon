## Context

`SitePermissionService.resolve(siteId, access, capability)` already resolves a member-override → function-override → hardcoded-default chain to an `AccessLevel` (`VIEW`/`MANAGE`). Every write action across the seven capabilities already calls `requireManage(...)`, which throws `ForbiddenCapabilityException` for anything short of `MANAGE`. But confirmed by direct inspection: **every read (list/get) method across every capability calls only `siteAccessService.requireAccess(siteId, userId)` — basic "are you a member of this site at all" — never `resolve`/`requireManage`.** This is intentional per the existing spec ("View-only member can still read/view the board"), not a bug. Adding `HIDDEN` therefore requires adding a check that doesn't exist today, not adjusting one that does.

## Goals / Non-Goals

**Goals:**
- A member whose resolved level for a capability is `HIDDEN` cannot read that capability's data via the API, not just "the tab happens to be missing."
- The corresponding tab disappears from `SiteDetailView.vue` for that member.
- Company staff (admins) are completely unaffected.
- `VIEW`'s existing "can still read" behavior is unchanged for every capability.

**Non-Goals:**
- No change to write-path enforcement (`requireManage`) — `HIDDEN` behaves like `VIEW` there (neither is `MANAGE`, both already get rejected the same way).
- No building of the member-level override UI that the spec already describes but `SitePermissionsPanel.vue` never implemented (function-level only) — a pre-existing gap, unrelated to adding a third level to the *existing* function-level UI.
- No change to the `schedule` tab (an unimplemented placeholder, not backed by any capability) or the `permissions` tab (gated by company-admin status, not by a `PermissionCapability` at all).

## Decisions

### 1. A read is gated by the *same* capability its own write path already uses
Rather than inventing a new mapping, each service's read methods get a `permissionService.requireVisible(siteId, access, CAPABILITY)` call using whatever `CAPABILITY` that same service's `requireManage` calls already use. Concretely: `SiteDocumentProjectService` → `DOCUMENT_PROJECTS`; `DailyReportService` → `DAILY_REPORT`; `EquipmentService` → `EQUIPMENT`; `MaterialService` → `ORCAMENTO_MANAGE` (its own `requireManage` already checks this, not `EQUIPMENT`, despite the intuitive association — kept consistent rather than "fixed" here, that's a separate naming question); `PurchaseRequestService` → `PURCHASE_REQUEST`; `OrcamentoService` → `ORCAMENTO_MANAGE`; `TaskCardService` → `TASKS`; `SiteMembershipService` → `TEAM_MANAGE`. This is a mechanical, low-risk rule — no new judgment calls per method, and it can never desync from the write gate since it's the same enum constant.

### 2. One `requireVisible` call per "entry point" method, not every sub-resource fetch
`OrcamentoService.listLineItems(orcamentoId)`/`listApprovals(orcamentoId)` take no `siteId`/`actingUserId` at all — confirmed via `OrcamentoController` that they're only ever called *after* `orcamentoService.get(id, user.getId())` already succeeded for the same orçamento in the same request. Gating `get()` (and `list()`) is therefore sufficient; no signature change needed for the two sub-resource methods. The same pattern holds for `DailyReportService`'s workforce/equipment/activity/occurrence/material sub-lists — each already takes the report id after a prior access-checked fetch — verified per-method rather than assumed.

### 3. `requireVisible` reuses `ForbiddenCapabilityException`, not a new exception type
`HIDDEN` and a blocked write both mean "you don't have the access this action needs" — same HTTP 403 shape, same existing handler. A dedicated `CapabilityHiddenException` would add a class and a handler entry for zero behavioral difference.

### 4. New `GET /api/sites/{siteId}/permissions/mine`, open to any active site member
Distinct from the existing `GET /api/sites/{siteId}/permissions` (company-staff-only, lists configured *overrides* for the admin UI). This one resolves all seven capabilities for the *caller themselves* via `SitePermissionService.resolveAll`, gated only by `siteAccessService.requireAccess` (any active member may know their own access) — needed because nothing today gives the frontend this visibility at all.

### 5. `SiteDetailView.vue` maps each tab to its capability and filters both the nav button and the panel
`team→TEAM_MANAGE, dailyReport→DAILY_REPORT, projects→DOCUMENT_PROJECTS, equipment→EQUIPMENT, purchaseRequests→PURCHASE_REQUEST, orcamentos→ORCAMENTO_MANAGE, tasks→TASKS`. A computed `visibleTabs` set (default to *visible* whenever "my permissions" hasn't loaded yet or a tab's capability is absent from the map, so nothing flashes hidden before the fetch resolves) drives both `v-if`s. `activeTab`'s initial value switches from the hardcoded `'team'` to the first entry of `visibleTabs`, falling back to `'team'` if every capability is somehow hidden (shouldn't happen given defaults, but keeps the view from rendering with no active tab).

## Risks / Trade-offs

- **[Risk] A member hidden from everything sees an empty tab bar.** → Accepted: that's the admin's explicit configuration; not this change's problem to second-guess (no default forces at least one tab visible).
- **[Trade-off] Two nearly-identical permission endpoints (`/permissions` staff-only list, `/permissions/mine` self-resolve).** → Accepted: different auth (staff-only vs. any member) and different shape (raw override rows vs. resolved levels) make merging them into one endpoint more confusing, not less.
