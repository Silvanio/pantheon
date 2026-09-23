## Why

Entering an obra today drops the user straight into whichever section-tab happens to be first visible (Equipe, Diário de Obra, ...) — there is no at-a-glance view of how that obra is doing: how many pending approvals, how the schedule is progressing, what was recently added. Users have to click into each section one at a time to find out. A summary dashboard, shown first when entering an obra, gives that overview immediately.

## What Changes

- New "Resumo" view shown first when entering an obra, on both `pantheon-web` and `pantheon-mobile`: cards for Cronograma progress, Diário de Obra, Pedido de Compra, Orçamentos, Equipamentos, Projetos, Tasks, and Equipe, each showing a count/stat and, for Pedido de Compra/Orçamentos/Tasks/Projetos, the 3 most recently created items (flagged "Novo" if created in the last 7 days).
- A "Pendências" highlight combining Pedido de Compra awaiting approval + draft Orçamentos, surfaced prominently.
- New backend endpoint aggregating all of this in one request, each section respecting the viewer's resolved capability visibility (omitted, not just empty, when `HIDDEN`) — reusing the paginated `list(...)` methods added for Pedido de Compra/Orçamentos/Diário de Obra/Equipamento (`add-on-demand-pagination-to-obra-lists`) for their counts and "recent" slices, and `ScheduleService.computeProgress` for the Cronograma card.
- Minimal new backend support only where it doesn't exist yet: a lightweight, permission-gated "recent + count" read path for Tasks and Projetos (neither has one today), and a member-count read for Equipe.

## Capabilities

### New Capabilities
- `obra-summary-dashboard`: the aggregated per-obra summary endpoint and its web/mobile presentation.

### Modified Capabilities
None — this reads existing data through existing capabilities' visibility rules; it doesn't change any of their requirements.

## Impact

- **Backend (`pantheon-service`)**: new `GET /api/construction-sites/{siteId}/summary` endpoint, `SiteSummaryController`, `SiteSummaryService` (composes existing services), new DTOs; `TaskCardRepository` and `SiteDocumentProjectRepository` gain a count method and a "top 5 by createdAt desc" method each; `SiteMembershipRepository` gains a count method.
- **Frontend (`pantheon-web`)**: new `SiteSummaryPanel.vue`, mounted as a new first tab (`'summary'`) in `SiteDetailView.vue`'s `TAB_ORDER`/`TAB_CAPABILITY`, made the default landing tab; a small composable for the summary fetch.
- **Mobile (`pantheon-mobile`)**: `SiteHomeScreen` gains a summary/stats section above its existing entry-card grid, backed by the same endpoint.
- No new dependencies, no database migration (all fields already exist; only new query methods on existing tables).
