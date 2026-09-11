## Why

Obras have no lightweight way to track "what needs to get delivered and by when" beyond free-text daily report entries. A simple, obra-scoped Trello-style board — cards moving across a small set of statuses — gives teams a visual way to track deliveries without introducing a new heavyweight workflow. Keeping the board's column structure company-wide (admin-defined once) instead of per-obra keeps it simple to reason about and consistent across every obra, while giving the company owner a single aggregated view of everything in flight.

## What Changes

- New **Pedido/Task board** ("Tasks") tab inside each obra, rendered as columns of cards (Trello-like).
- New **company-level task column configuration**: the set of columns (name, order) is defined once per company, by a company `ADMIN` only, from the company settings screen (top-right profile menu → "Editar empresa") — not from inside an obra, and not gated by the per-obra permission system.
- Every obra shares its company's column structure; **task cards themselves are obra-scoped** — each obra has its own independent set of cards placed into those shared columns.
- Inside an obra, permitted members can create cards, move cards between columns, attach labels, and add comments. Access to these actions is controlled by the existing per-site permission system (a new `TASKS` capability on `PermissionCapability`), same mechanism as `EQUIPMENT`/`PURCHASE_REQUEST`/`ORCAMENTO_MANAGE`. No new granularity — one capability gates all task actions for now.
- New **global Tasks board** shortcut on the dashboard, visible only to company `ADMIN` users, showing every obra's cards together in one board. Each card carries a colored label naming its obra (color deterministically derived per obra, not stored) so cards from different obras are visually distinguishable without mixing real data.
- Initial scope is intentionally simple: no due dates, assignees, attachments, or card archiving yet — just columns, cards, labels, and comments.

## Capabilities

### New Capabilities
- `company-task-columns`: Company-wide, admin-only configuration of the task board's columns (create, rename, reorder, delete), managed from company settings and shared by every obra under that company.
- `obra-tasks-board`: Per-obra Trello-like board — cards placed into the company's columns, with create/move/label/comment actions gated by the site permission system.
- `global-tasks-board`: Admin-only aggregated read view combining every obra's task cards into one board, with a deterministic per-obra color label.

### Modified Capabilities
- `obra-permission-management`: `PermissionCapability` enum gains a `TASKS` value; the per-`ConstructionFunction` default access matrix is extended with a default for it.

## Impact

- **pantheon-service**: new entities (`TaskColumn` company-scoped, `TaskCard`/`TaskLabel`/`TaskComment` obra-scoped), Flyway migrations starting at `V44`, new controllers/services/DTOs/repositories/exceptions following the existing layered pattern (see `PurchaseRequestController`/`PurchaseRequestService`), `PermissionCapability` enum + `SitePermissionService.DEFAULTS` update, company-admin authorization reusing the existing `requireAdmin(companyId, userId)` pattern (`CompanyService`/`PlanService`/`ConstructionSiteService`).
- **pantheon-web**: new "Tasks" tab in `SiteDetailView.vue`, new `CompanyTaskColumnsPanel.vue` rendered from `CompanySettingsView.vue`, new admin-only dashboard shortcut + global board view, new composables (`useTaskColumns`, `useTaskCards`, `useGlobalTasksBoard`). The stale `PermissionCapability` union in `useSitePermissions.ts` (still listing retired `EQUIPMENT_MATERIAL`/`MATERIAL_REQUEST`/`MATERIAL_APPROVAL`) is corrected to match the backend enum, including the new `TASKS` value.
