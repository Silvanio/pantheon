## 1. Permission parity audit and fixes

- [x] 1.1 Compare `pantheon-web`'s permission-gated UI logic (`SiteDetailView.vue`, `PurchaseRequestDetailView.vue`, `PurchaseRequestPanel.vue`) against the mobile equivalents
- [x] 1.2 Fix "Permissões" entry card to gate by company-admin role, matching web's `isCompanyAdmin`
- [x] 1.3 Fix purchase-request pending-step selection to use current-cycle + lowest-step-order (mirrors web's `currentPendingApproval`)
- [x] 1.4 Fix the company-staff-with-no-`SiteMembership` approval bypass to require `MANAGE` specifically
- [x] 1.5 Add the missing "Enviar para aprovação", "Concluir", and "Novo pedido" actions, gated to match web

## 2. Offline core infrastructure

- [x] 2.1 Add `sqflite`, `path`, `connectivity_plus`, `uuid` dependencies
- [x] 2.2 `OfflineDb` (SQLite schema: `cache`, `outbox` tables)
- [x] 2.3 `CacheStore` (get/put keyed by request path+query)
- [x] 2.4 `OutboxStore` + `OutboxEntry` model (plain-JSON mutations and file uploads)
- [x] 2.5 `OutboxController`: connectivity tracking, enqueue/enqueueUpload, auto-drain on reconnect, manual `syncNow`, discard
- [x] 2.6 `ApiClient.get`'s `offlineCapable` flag (default true) — cache write-through + fallback-on-failure, or neither when false
- [x] 2.7 `ApiClient.mutateQueueable` / `uploadQueueable` — attempt live, enqueue on connectivity failure

## 3. Offline UX

- [x] 3.1 `confirmProceedOffline` — ask-before dialog when already offline
- [x] 3.2 `showOfflineSavedDialog` — inform-after dialog for the mid-request connectivity-drop case
- [x] 3.3 `requireOnline` — blocking dialog for actions that must never be queued
- [x] 3.4 `SyncStatusBadge` — floating indicator, wired into `main.dart`'s `MaterialApp.builder`
- [x] 3.5 `SyncScreen` (`/sync` route) — pending list, manual sync, discard

## 4. Wiring per module

- [x] 4.1 Diário de Obra: create, submit, upload photo → queueable; list/detail → cache-capable
- [x] 4.2 Pedido de Compra: create → queueable; list/detail/comparison → cache-capable; submit/approve/reject/conclude → `requireOnline` (never queued)
- [x] 4.3 Orçamentos: list/detail → cache-capable (no write actions exist yet)
- [x] 4.4 Tasks: board fetch → `offlineCapable: false`; move/create card → `requireOnline`
- [x] 4.5 Projetos: folder contents fetch → `offlineCapable: false`
- [x] 4.6 Permissões: overrides list fetch → `offlineCapable: false`

## 5. Verification

- [x] 5.1 `flutter analyze` clean
- [x] 5.2 `flutter test` passes
- [x] 5.3 Live verification on Android emulator with airplane mode: cache-fallback (Dashboard, Diário list), offline-queued report creation with confirm/inform modals, badge pending-count, auto-sync on reconnect (confirmed the record landed on the real backend), Tasks board correctly failing offline (no cache)
- [x] 5.4 README updated with the offline-support scope and activation notes
