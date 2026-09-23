## Why

Diário de Obra and Equipamento currently fetch every record for a site in one request and render it all at once — on a long-running obra with hundreds of daily reports or equipment items, that means an ever-growing payload and an ever-growing table with no way to bound it. Pedido de Compra and Orçamentos already solved this with real server-side pagination (`page`/`size`, `Page<T>` response, prev/next UI); the other two listing screens should follow the same pattern instead of loading everything.

## What Changes

- Diário de Obra's list endpoint (`GET /api/construction-sites/{siteId}/daily-reports`) and Equipamento's list endpoint (`GET /api/construction-sites/{siteId}/equipment`) become paginated (`page`/`size` query params, `Page<T>` response), sorted by `createdAt` descending (newest first) — mirroring Pedido de Compra/Orçamentos exactly.
- `DailyReportsPanel.vue` and `EquipmentPanel.vue` gain the same prev/next pagination footer already used by `PurchaseRequestPanel.vue`/`OrcamentoListPanel.vue`, fetching one page (10 or 20 rows) on demand instead of the full history.
- `pantheon-mobile`'s `DailyReportRepository`/`EquipmentRepository` are updated to parse the new `Page<T>` envelope (they were parsing a raw JSON array), using the same generous-single-page approach already used for Pedido de Compra on mobile (no mobile pagination UI added — out of scope, matches existing mobile precedent).
- Orçamentos' existing page size is normalized from 12 to 20 to match the other three lists (Pedido de Compra, Diário de Obra, Equipamento), so every obra list uses the same page size.
- **BREAKING**: the two changed endpoints' response shape changes from a plain JSON array to a `Page<T>` envelope (`{content, totalElements, totalPages, number, size}`). Every consumer (web and mobile) is updated in this same change.

## Capabilities

### Modified Capabilities
- `daily-construction-report`: the "Daily report listing and detail" requirement's listing behavior becomes paginated and sorted by creation date (was: returns every report at once, ordered by date).
- `equipment-material-registry`: the "List equipment of a construction site" requirement becomes paginated and sorted by creation date (was: returns every equipment record at once, unordered).

## Impact

- **Backend (`pantheon-service`)**: `DailyReportRepository`/`EquipmentRepository` gain `Pageable`-based finder methods sorted by `createdAt` desc; `DailyReportService.list`/`EquipmentService.list` and their controllers take `page`/`size` params and return `Page<T>`.
- **Frontend (`pantheon-web`)**: `useDailyReports.ts`/`useEquipment.ts` composables return `PageResponse<T>` (reusing the type already defined in `usePurchaseRequests.ts`); `DailyReportsPanel.vue`/`EquipmentPanel.vue` gain pagination state and the shared footer UI; `OrcamentoListPanel.vue`'s `PAGE_SIZE` changes from 12 to 20.
- **Mobile (`pantheon-mobile`)**: `DailyReportRepository.list`/`EquipmentRepository.list` parse `PageResponse<T>` (already defined in `lib/core/models/page_response.dart`) instead of a raw array; list screens unchanged otherwise (single generously-sized page, same pattern as `PurchaseRequestRepository.list`).
- No new dependencies, no database migration needed (`createdAt` already exists on both entities).
