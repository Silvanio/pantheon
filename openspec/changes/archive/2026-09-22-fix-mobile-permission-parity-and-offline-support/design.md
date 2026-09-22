## Context

`add-flutter-mobile-app` shipped a working app, but two things surfaced once it was validated against the real web app and against how it'd actually be used: permission-gated UI drifted from `pantheon-web` in a few concrete, checkable spots, and there was no accommodation for the intermittent connectivity a construction site implies.

## Goals / Non-Goals

**Goals:**
- Make every permission-gated screen/action in the mobile app match `pantheon-web`'s resolved-access-level behavior exactly, verified by direct comparison of the two codebases' gating logic.
- Let Diário de Obra, Pedido de Compra (view/create), and Orçamentos (view) work offline: last-known data on reads, an automatically-draining local outbox on writes.
- Make the offline behavior transparent: a confirm-before-queueing modal, an inform-after-the-fact modal for the rarer race, and a persistent status indicator with a screen to inspect/force-sync the queue.

**Non-Goals:**
- Offline support for Tasks, Projetos, Permissões, or the Pedido de Compra approval actions themselves — explicitly decided against, since these are either administrative/config surfaces that should always reflect the live source of truth, or authoritative decisions (approve/reject/conclude) that shouldn't be made on stale data or queued silently.
- Conflict resolution for concurrent edits (e.g., two devices editing the same Diário de Obra offline) — out of scope; the outbox replays in creation order and a later conflict surfaces as a normal per-item sync error the user can inspect on the sync screen.

## Decisions

- **Cache-fallback is opt-out, not opt-in, at the `ApiClient.get` level.** Every GET is cached by default; call sites that must never show stale/local data pass `offlineCapable: false`, which skips both the cache write and the fallback read. This means new screens are offline-capable by default (matching the product's overall intent) and exclusions are a explicit, visible one-line marker at each call site that shouldn't be.
- **Two distinct dialogs, not one.** `confirmProceedOffline` (ask first, when we already know we're offline) and `showOfflineSavedDialog` (inform after, for the rarer case connectivity dropped mid-request) are separate functions rather than one "queued" notification, because the first is a yes/no decision the user should make deliberately and the second is a fait accompli that only needs acknowledgement — collapsing them into one snackbar (the original implementation) buried a decision the user explicitly asked to be surfaced as a modal.
- **`requireOnline` blocks rather than queues** for approvals/tasks — a third, distinct helper, deliberately not reusing the queueing path, since queueing here would be silently doing something the user asked to be prevented from doing.
- **The outbox lives in SQLite (`sqflite`), not `shared_preferences`/Hive**, because it needs simple relational querying (order by creation time, per-entry error state) and holds an unbounded, growing list of heterogeneous entries (plain-JSON mutations and file uploads) — a proper table fits better than a serialized blob.
- **Connectivity is tracked via `connectivity_plus`, with the outbox auto-draining on the offline→online transition**, rather than polling — matches user expectations ("volta a internet, encontra do que enviou") without a background timer.

## Risks / Trade-offs

- [A queued file upload's local file could be gone by the time the outbox replays it (OS cache eviction)] → `OutboxController.syncNow` checks the file exists before attempting the upload and records a clear "arquivo local não encontrado" error on that entry instead of looping forever, surfaced on the sync screen.
- [Riverpod's `FutureProvider.family` keeps a screen's last `AsyncValue` (including an error) across navigations, so a screen that failed while offline won't automatically retry just because connectivity returned] → existing `RefreshIndicator` pull-to-refresh (already present on every list screen) is the user's way to retry; this is a pre-existing characteristic of the app's provider usage, not introduced by this change.
- [Excluding Tasks/Projetos/Permissões/approvals from offline means those screens are unusable without connectivity] → deliberate, per the "Non-Goals" above — flagged to the user as a considered trade-off, not an oversight.

## Migration Plan

Additive only on the mobile side (new local SQLite database, created on first run); no backend or web changes. Nothing to roll back beyond reverting the mobile app version.
