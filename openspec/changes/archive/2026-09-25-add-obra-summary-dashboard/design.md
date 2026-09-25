## Context

Eight obra sections already exist, each gated by its own `PermissionCapability`. Four of them (Pedido de Compra, Orçamentos, Diário de Obra, Equipamento) already have paginated, `createdAt`-descending-sorted `list(siteId, actingUserId, Pageable)` service methods (added in `add-on-demand-pagination-to-obra-lists`), each already enforcing `requireVisible`/throwing `ForbiddenCapabilityException` when the capability is `HIDDEN`. `ScheduleService.computeProgress(siteId)` (added in `add-construction-schedule-gantt`) returns the obra's schedule completion %, `null` when there's no schedule yet. Tasks and Projetos ("Projetos" = `SiteDocumentProject`, a folder-based tree) have no comparable lightweight "count + recent" read path — Tasks' only read is `TaskCardService.getBoard`, which loads the entire board (columns, cards, labels, assignees, comment/attachment counts) and isn't sorted by creation date; Projetos only has parent/child tree queries.

## Goals / Non-Goals

**Goals:**
- One request from the client gets everything the summary view needs — no N separate calls into each section on every obra visit.
- Every card's visibility mirrors that section's own tab visibility exactly (same capability, same HIDDEN behavior) — the summary must never leak a count or recent-item title from a section the viewer can't otherwise see.
- Reuse the four already-paginated `list(...)` methods as-is for their counts and recent slices, rather than querying their repositories a second, parallel way.
- Both web and mobile show this summary on entering an obra.

**Non-Goals:**
- No "since your last visit" read-tracking — "Novo" is a stateless, purely date-based flag (created within the last 7 days), not personalized per-viewer read state. Simpler, and avoids a new persisted "last seen" concept.
- No drill-down filtering from the summary (e.g. clicking "3 novos pedidos" doesn't open a pre-filtered Pedido de Compra list) — v1 recent items are informational; clicking one navigates straight to that item's own detail page, which already exists.
- No summary for Permissões (it's a company-admin configuration screen, not obra activity) or for the "Novo pedido"/creation flows themselves.
- No change to any existing section's own requirements — this only reads.

## Decisions

**One aggregation service composing the existing per-capability services, not a new cross-cutting query layer.** `SiteSummaryService` calls `purchaseRequestService.list(siteId, userId, null, null, PageRequest.of(0, 3))`, `orcamentoService.list(...)`, `dailyReportService.list(...)`, `equipmentService.list(...)` exactly as their own controllers do, catching `ForbiddenCapabilityException` per section and mapping that to "omit this section" (`null` field) rather than letting the whole summary request fail. This means every section's permission logic lives in exactly one place (its own service) — the summary never re-implements a visibility check that could drift from the real one.

**Tasks and Projetos get minimal new repository methods, not a new service abstraction.** Rather than building a paginated `list` for Tasks/Projetos matching the other four (which would be over-scoped for a feature that only needs "count + top 3 recent" and isn't asked to paginate these two lists elsewhere), `SiteSummaryService` does its own `requireVisible(TASKS)`/`requireVisible(DOCUMENT_PROJECTS)` check and calls two new, purpose-built repository methods each (`countByConstructionSiteId`, `findTop5ByConstructionSiteIdOrderByCreatedAtDesc` — fetch 5, use first 3, so a future "show more" isn't a schema change). This is the one place summary-specific permission-check code exists; kept to exactly two sections, both documented inline as to why they differ from the other four.

**"Pendências" is computed client-side from fields already in the summary response, not a separate backend field.** Awaiting-approval Pedido de Compra count and draft-Orçamento count are already present in their respective summary sections (same definitions `PurchaseRequestPanel.vue`'s stats already use: `ORCADO` + `submittedAt != null`, and `status === 'DRAFT'`). Adding a redundant backend "pendingCount" field would be a second source of truth for numbers already in the payload — the frontend/mobile just sums the two.

**"Novo" is `createdAt` within 7 days of the request, computed per item, not a stored flag.** A `RecentItem` DTO carries its own `createdAt`; each client computes "is this new" the same way (`now - createdAt <= 7 days`) purely for a badge — no server-side boolean needed, and no clock-skew risk worth worrying about at this granularity.

**Reused generic shapes.** A `CountStat` record (`total: int`, `recent: List<RecentItem>`) is shared by Pedido de Compra, Orçamentos, Tasks, and Projetos sections (all "count + 3 recent items" shaped); Equipamento, Diário de Obra, Equipe, and Cronograma get their own small records since each carries a different secondary stat (unavailable count, last report date, member count, percent complete) instead of a recent-items list.

## Risks / Trade-offs

- **[Risk] `SiteSummaryService` makes up to 8 permission checks and up to 6 small paginated queries per request — more DB round trips than any single existing endpoint.** → Mitigation: every query is `size=3` or a simple `count`/`findTop5`, all indexed on `construction_site_id`; this replaces what would otherwise be 6-8 separate client requests (one per section) with one server-side fan-out, a net reduction in round trips from the client's perspective, which is what actually matters for the "entering an obra" experience.
- **[Risk] Catching `ForbiddenCapabilityException` per section to mean "omit" conflates "truly hidden" with any other reason that exception could be thrown.** → Mitigation: `ForbiddenCapabilityException` today is thrown exclusively by `SitePermissionService.requireVisible`/`requireManage` for a resolved `HIDDEN` (or non-`MANAGE`, for write paths — not used here) capability; the summary only calls read paths, so the exception can only mean "hidden" in this context.

## Migration Plan

1. Backend: two new repository methods each on `TaskCardRepository`/`SiteDocumentProjectRepository`, one new count method on `SiteMembershipRepository`; `SiteSummaryService`/`SiteSummaryController`/DTOs; run `./mvnw test`.
2. Frontend: `useSiteSummary.ts` composable, `SiteSummaryPanel.vue`, wire as the new default first tab in `SiteDetailView.vue`; `vue-tsc`/`npm run build`.
3. Mobile: summary section in `SiteHomeScreen`, backed by the same endpoint; `flutter analyze`/`flutter test`.
4. No data migration. Rollback is purely additive-revert (remove the new tab/section and endpoint) — no existing behavior changes.
