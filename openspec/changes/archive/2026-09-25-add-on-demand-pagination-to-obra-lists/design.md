## Context

`PurchaseRequestService.list`/`OrcamentoService.list` already establish the exact pattern to copy: a `Pageable`-taking service method, `PageRequest.of(page, size, Sort.by(DESC, "createdAt"))`, a `Page<T>` return type, and a controller with `@RequestParam(defaultValue = "0") int page` / `@RequestParam(defaultValue = "20") int size`. `DailyReportService.list`/`EquipmentService.list` instead return a full `List<T>` from a simple (non-`Pageable`) repository finder. Both entities already have `createdAt`; no schema change is needed. `pantheon-mobile` already has a `PageResponse<T>` Dart class (used for Pedido de Compra) and a mobile-side precedent of not building real pagination UI — it just requests one generously-sized page and renders it flat.

## Goals / Non-Goals

**Goals:**
- Daily-report and equipment listing become real server-side pagination, identical in shape and behavior to the existing Pedido de Compra/Orçamentos implementation.
- Every list is sorted by creation date descending everywhere (backend default), so the newest record is always first.
- Every consumer of the two changed endpoints (web, mobile) keeps working after the response-shape change — updated in this same change, not left broken.

**Non-Goals:**
- No mobile pagination UI (prev/next, page indicator) for these two lists — mobile has never had this for any list, including the already-paginated Pedido de Compra; adding it now is a separate, unrequested scope.
- No filtering (date/status/etc.) added to these two endpoints — Pedido de Compra/Orçamentos have filters because their screens already had filter UI; Diário de Obra/Equipamento don't, and none was requested.
- No change to `DailyReportRepository.findByConstructionSiteIdAndReportDate` or any other finder unrelated to the plain "list everything for a site" method.

## Decisions

**Sort by `createdAt`, not `reportDate`.** Diário de Obra's current unpaginated finder sorts by the report's own business date (`reportDate`), not by when the row was created. The user explicitly asked for creation-date ordering ("a ordenação é por data de criação... deve sempre mostrar o ultimo que foi criado") applied uniformly across all four lists — matching Pedido de Compra/Orçamentos, which already sort by `createdAt`. In practice these two orderings coincide for almost all reports (people file a report for the day they're filing it), so this is a low-risk, explicitly-requested change, not a behavior regression.

**Reuse `Page<T>`/`PageResponse<T>`, don't invent a new envelope.** Both the backend (`org.springframework.data.domain.Page`) and the frontend (`PageResponse<T>` in `usePurchaseRequests.ts`) and mobile (`PageResponse<T>` in `page_response.dart`) already have a generic paginated envelope in active use by two other resources. Reusing it (rather than a bespoke shape for these two) keeps the four obra lists consistent and lets `DailyReportsPanel.vue`/`EquipmentPanel.vue` copy `PurchaseRequestPanel.vue`'s pagination footer verbatim.

**Normalize Orçamentos' page size to 20.** The user's instruction applies "10 or 20" uniformly to all four lists; Orçamentos currently uses 12. Changing a `const PAGE_SIZE` value is a one-line, purely cosmetic fix (page count changes, nothing else does) — included here rather than filed separately, since it's the same instruction being fulfilled.

**Mobile gets a compatibility fix, not new UX.** The two changed endpoints are shared with `pantheon-mobile`. Since their JSON shape changes from an array to `{content, totalElements, ...}`, mobile's `DailyReportRepository.list`/`EquipmentRepository.list` would silently break (a cast from `List<dynamic>` to a JSON object) if left untouched. Fixing that parsing is mandatory, not optional scope — but building actual mobile pagination controls is not requested anywhere in this change, and mobile's own Pedido de Compra screen (already backed by a paginated endpoint) has never had them either; mobile keeps requesting one page sized generously enough (`size: 100`) to behave the same as before for typical obras.

## Risks / Trade-offs

- **[Risk] A site with more daily reports or equipment than the mobile app's single-page size (100) will silently show only the newest 100 on mobile, with no way to see older ones.** → Mitigation: matches the exact trade-off already accepted for Pedido de Compra on mobile (`size: 50` there); consistent, pre-existing product decision, not a new gap introduced by this change. Flagged here for visibility, not blocking.
- **[Risk] Changing Diário de Obra's sort from `reportDate` to `createdAt` could reorder history for a site where reports were filed out of chronological order (e.g. a backfilled report for last week).** → Mitigation: explicitly requested by the user; the "Novo relatório" date picker prevents duplicate `reportDate`s per site, so this only affects display order, not data integrity.

## Migration Plan

1. Backend: repository finder + service + controller changes for both resources; run `./mvnw test`.
2. Frontend: composable + panel changes for both resources, plus the Orçamentos page-size tweak; `vue-tsc`/`npm run build`.
3. Mobile: repository parsing fix for both resources; `flutter analyze`/`flutter test`.
4. No data migration, no backfill — purely a query/response-shape and UI change on already-existing columns.
5. Rollback: revert the three sub-changes together (they must move in lockstep since the endpoint contract changed) — there is no partial-rollback state where only the backend or only a client is on the new shape.
