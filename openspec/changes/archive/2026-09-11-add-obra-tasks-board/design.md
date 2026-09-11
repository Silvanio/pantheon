## Context

The project already has two authorization layers that this feature must reuse rather than reinvent:

1. **Per-obra capability permissions** (`PermissionCapability` + `SitePermissionService`): a member/function/default cascade resolving `AccessLevel.VIEW` or `AccessLevel.MANAGE` per site, per capability. Company staff always resolve to `MANAGE`. Some capabilities (`PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`) treat `VIEW` as "no access"; others (`DAILY_REPORT`) give real read-only access at `VIEW`. Tasks belongs in the second group — everyone in the obra should be able to *see* the board (that's the point: tracking deliveries), only `MANAGE` should be required to create/move/label/comment.
2. **Company-admin checks**: `CompanyService`, `PlanService`, and `ConstructionSiteService` each already have a private `requireAdmin(companyId, userId)` that loads the caller's `CompanyMembership` and throws `NotCompanyAdminException` unless `role == CompanyRole.ADMIN`. There's no shared helper — each service duplicates it. The task-column and global-board services will follow this same duplication pattern rather than introduce a new shared utility.

The obra tab pattern (`SiteDetailView.vue`'s `Tab` union + nav button + `v-if` panel) and the layered backend pattern (thin controller → `Response.from(entity)` mapping, service owns `requireSite`/`requireAccess`/`requireManage`, plain JPA entity with service-generated `UUID`, thin repository) are both already established by the Purchase Request / Orçamento work and will be followed as-is.

## Goals / Non-Goals

**Goals:**
- A Trello-like board per obra: shared company-wide columns, obra-scoped cards.
- Admin-only column configuration living in company settings, outside any obra.
- Card create/move/label/comment gated by a new single `TASKS` site-permission capability.
- An admin-only aggregated board across all obras on the dashboard, with a stable-but-unstored per-obra color label.

**Non-Goals (this iteration):**
- Due dates, assignees, attachments, or archiving on cards.
- Fine-grained capability split (create vs. move vs. label vs. comment) — one `TASKS` capability covers all.
- Real-time board sync (no websockets) — board refreshes on navigation/action, same as other panels.
- Drag-and-drop position precision under concurrent edits — last-write-wins is acceptable at current scale.
- Notifications on card/column changes.

## Decisions

**Columns are company-scoped, not obra-scoped.** A `TaskColumn` row belongs to a `companyId`, not a `constructionSiteId`. Every obra under that company renders the same column set. This matches the requirement directly and keeps configuration in one place (`CompanySettingsView.vue`, new `CompanyTaskColumnsPanel.vue`) instead of duplicating column setup per obra. Alternative considered: per-obra column overrides — rejected as unnecessary complexity for a "simpler version" whose goal is just delivery tracking.

**Column CRUD is company-admin gated via a duplicated `requireAdmin`, not via `SitePermissionService`.** Matches the user's instruction that this is a "fixed feature, admin only," independent of the per-obra permission matrix. `TaskColumnService` gets its own private `requireAdmin(companyId, userId)`, mirroring `CompanyService`/`PlanService`/`ConstructionSiteService` rather than factoring out a shared helper — consistent with how the codebase already handles this (no premature abstraction).

**Card/label/comment actions are gated by one new `TASKS` capability, modeled like `DAILY_REPORT` (real `VIEW`), not like `PURCHASE_REQUEST` (`VIEW` = denied).** Reading the board (`GET` endpoints) only requires `AccessLevel.VIEW`; creating a card, moving a card, creating/attaching a label, and posting a comment all require `AccessLevel.MANAGE`. Default matrix mirrors `DAILY_REPORT`'s defaults exactly (`ENGINEER`/`ARCHITECT`/`SITE_FOREMAN` → `MANAGE`, `CLIENT`/`SERVICE_PROVIDER` → `VIEW`), since delivery-tracking involvement maps to the same roles as daily-report involvement.

**Cards, labels, and comments are obra-scoped and reference the shared column by id.** `TaskCard.constructionSiteId` + `TaskCard.columnId` (FK to the company's `TaskColumn`). No cross-obra leakage: every obra query filters by `constructionSiteId`. Labels (`TaskLabel`) are also obra-scoped and reusable within that obra (create once, attach to many cards via a `TaskCardLabel` join table) — this is a separate, real per-obra label a user assigns (e.g. "Urgente", red), distinct from the global-board's obra-origin label described next.

**Column deletion is blocked while any card (in any obra) still references it.** Deleting a shared column is destructive across every obra at once, so `TaskColumnService.delete` throws `TaskColumnInUseException` if `TaskCardRepository.existsByColumnId(columnId)` is true. The admin must move or clear cards first (out of scope to build a "reassign on delete" flow this iteration). Alternative considered: cascade-delete cards — rejected as too destructive for a shared, cross-obra structure.

**Global board's per-obra color is computed, not stored.** "Cor aleatória" is read as *visually distinct per obra*, not *re-randomized every page load* (which would be confusing — the same obra's cards would flicker between colors on refresh). The global-board endpoint derives a color per `constructionSiteId` by hashing the site id against a small fixed palette (12–16 hand-picked hex values) and picking `palette[hash(siteId) % palette.length]`. Same obra → same color, always, with zero new persisted state. Alternative considered: a `colorHex` column on `ConstructionSite` — rejected as unnecessary persisted state for a purely presentational, admin-only view.

**Global board is a dedicated read endpoint, not a client-side merge of per-obra calls.** `GET /api/companies/{companyId}/tasks-board` (admin-gated, same `requireAdmin` pattern) returns every column, plus every card across every obra in that company annotated with `siteId`/`siteName`/the derived color — one request, one board. Alternative considered: fetch obra list then call each obra's task-card endpoint — rejected as N+1 and unnecessarily chatty for a single aggregated view.

**Card ordering within a column uses a simple integer `position`, set to end-of-column on create and rewritten on move.** No fractional-indexing scheme — acceptable given single-admin/small-team scale and the "simpler version" instruction.

**Frontend move interaction: native HTML5 drag-and-drop, no new dependency.** Vue's existing DOM event handling covers `dragstart`/`dragover`/`drop` for card-to-column moves, keeping the Trello-like feel without adding a drag-and-drop library.

## Risks / Trade-offs

- **[Shared columns mean one admin's rename/reorder/delete affects every obra at once]** → Column delete is blocked while in use anywhere (see above); rename/reorder are non-destructive so left unrestricted beyond admin-only access.
- **[Palette collisions once obra count exceeds the palette size]** → Two obras could render the same color on the global board. Acceptable at current scale (single company, handful of obras); documented as a known limitation, not solved with persisted state this iteration.
- **[Integer `position` can produce ties under concurrent moves]** → Last write wins; no optimistic locking added. Acceptable given the "simpler version" instruction and lack of concurrent multi-admin usage today.
- **[Global board endpoint loads every obra's cards in one response]** → Fine at current data volume; flagged as a future pagination/streaming concern if obra/card counts grow significantly.
- **[Stale `PermissionCapability` union in `useSitePermissions.ts` predates this change]** → Corrected as part of this change (adding `TASKS` requires touching that file anyway), not left further out of sync.

## Migration Plan

Flyway migrations, continuing from `V43`:
- `V44__create_task_column_table.sql` — `task_column` (id, company_id FK, name, position, created_at).
- `V45__create_task_card_table.sql` — `task_card` (id, construction_site_id FK, column_id FK → task_column, title, description nullable, position, created_by, created_at, updated_at).
- `V46__create_task_label_tables.sql` — `task_label` (id, construction_site_id FK, name, color_hex, created_at) and join table `task_card_label` (card_id FK, label_id FK, PK on both).
- `V47__create_task_comment_table.sql` — `task_comment` (id, card_id FK → task_card, author_id, body, created_at).

No data migration needed — this is entirely new, additive schema; nothing existing changes shape. Rollback is a straight `DROP TABLE` in reverse order if needed pre-release; no production data at stake for a brand-new feature.

## Open Questions

None blocking — defaults chosen above (mirroring `DAILY_REPORT` permission defaults, hash-based palette color, blocked-on-in-use column delete) are reasonable for a first "simpler version" and can be revisited once real usage surfaces friction.
