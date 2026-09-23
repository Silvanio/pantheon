## 1. Backend: Diário de Obra pagination

- [x] 1.1 `DailyReportRepository.java`: replace `findByConstructionSiteIdOrderByReportDateDesc` with `Page<DailyReport> findByConstructionSiteId(UUID constructionSiteId, Pageable pageable)`.
- [x] 1.2 `DailyReportService.list`: change signature to `list(UUID siteId, UUID actingUserId, Pageable pageable)`, build `PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(DESC, "createdAt"))` (mirroring `PurchaseRequestService.list`), return `Page<DailyReport>`.
- [x] 1.3 `DailyReportController.list`: add `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "20") int size`, return `ResponseEntity<Page<DailyReportResponse>>` via `.map(DailyReportResponse::from)`.

## 2. Backend: Equipamento pagination

- [x] 2.1 `EquipmentRepository.java`: replace `findByConstructionSiteId(UUID)` returning `List<Equipment>` with `Page<Equipment> findByConstructionSiteId(UUID constructionSiteId, Pageable pageable)`.
- [x] 2.2 `EquipmentService.list`: change signature to `list(UUID siteId, UUID actingUserId, Pageable pageable)`, build `PageRequest.of(..., Sort.by(DESC, "createdAt"))`, return `Page<Equipment>`.
- [x] 2.3 `EquipmentController.list`: add `page`/`size` request params (defaults 0/20), return `ResponseEntity<Page<EquipmentResponse>>`.

## 3. Backend tests

- [x] 3.1 Update/add tests for `DailyReportService.list` and `EquipmentService.list` (or their equivalent test files) confirming: paginated results, `createdAt` descending order (newest first), correct `totalElements`/`totalPages`.
- [x] 3.2 Run `./mvnw test` from `pantheon-service/` and confirm the full suite passes.

## 4. Frontend: composables

- [x] 4.1 `useDailyReports.ts`: import `PageResponse` from `usePurchaseRequests.ts` (or wherever it's re-exported), change `listReports(siteId)` to `listReports(siteId, { page, size }): Promise<PageResponse<DailyReport>>` with `page` defaulting to 0 and `size` to 20.
- [x] 4.2 `useEquipment.ts`: same change for `listEquipment(siteId, { page, size }): Promise<PageResponse<Equipment>>`.

## 5. Frontend: panels

- [x] 5.1 `DailyReportsPanel.vue`: add `page`/`totalPages`/`totalElements` refs and a `PAGE_SIZE = 20` constant, update `load()` to call the new paginated `listReports`, reset `page` to 0 on create, add the same prev/next pagination footer markup used in `PurchaseRequestPanel.vue` (reuse i18n key names under a `dailyReports.pagination.*` namespace).
- [x] 5.2 `EquipmentPanel.vue`: same changes for `listEquipment`, under an `equipment.pagination.*` i18n namespace.
- [x] 5.3 `OrcamentoListPanel.vue`: change `PAGE_SIZE` from `12` to `20`.
- [x] 5.4 Add the new `pagination.*` i18n keys (summary/previous/next) to `pt-BR.json` under `dailyReports` and `equipment`, mirroring `purchaseRequests.pagination.*`'s existing copy.

## 6. Mobile: repository compatibility

- [x] 6.1 `daily_report_repository.dart`: change `list(String siteId)` to parse `PageResponse<DailyReport>` (`lib/core/models/page_response.dart`) via `_client.get<Map<String, dynamic>>(...)`, request a generously-sized single page (`size: 100`, mirroring `PurchaseRequestRepository.list`'s `size: 50` precedent), and return `.content` so `DailyReportListScreen` needs no changes.
- [x] 6.2 `equipment_repository.dart`: same change for `list(String siteId)`.
- [x] 6.3 Run `flutter analyze` and `flutter test` from `pantheon-mobile/`.

## 7. Verification

- [x] 7.1 Backend: `./mvnw test` green.
- [x] 7.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green.
- [x] 7.3 Mobile: `flutter analyze` and `flutter test` green.
- [x] 7.4 Live check in the browser: Diário de Obra and Equipamento tabs both show only one page (10 or 20 rows) at a time with working prev/next controls, newest item first; confirm Orçamentos' page size changed to 20; confirm mobile's equipment/daily-report screens still load and show data (no crash from the response-shape change) — check via the Android emulator or a static reproduction of the new JSON shape if the emulator isn't available.
