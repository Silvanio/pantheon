## Context

`add-task-card-sse-events` (archived 2026-09-14) shipped `task-card-moved` as a company-scoped SSE event fanned out via RabbitMQ. Multi-account manual testing that day found the company-scope resolution incomplete (see Decisions), and the same testing session raised a related but separate bug in the board's own load path, plus a request to extend the same mechanism to card create/delete/edit.

## Goals / Non-Goals

**Goals:**
- Every user who can see an obra's Tasks board — company staff or site-only member alike — receives that company's `task-card-*` events.
- A card action's success is never masked by an unrelated, non-essential fetch failing.
- Creating, deleting, and editing a card (due date, assignees, labels, comments) is as real-time as moving one.

**Non-Goals:**
- Live comment *thread* sync (seeing a comment's body appear in an already-open detail modal). Only the comment *count* updates in real time; reading a new comment's text still requires reopening the card. A live comment thread is closer to a chat feature and deserves its own design.
- Extending real-time sync to the global (admin-only, cross-obra) Tasks board — unchanged from the original change's non-goals.
- Any change to who is *authorized* to perform an action (move/create/delete/edit) — this change only affects who is *notified* after an authorized action succeeds, plus one unrelated read-only fetch's error handling.

## Decisions

### 1. SSE company-scope resolution includes `SiteMembership`, not just `CompanyMembership`
`SseController.subscribe()` originally resolved a user's company scope from `CompanyMembership` alone. But `SiteAccessService` — the actual authority for "can this user act on this obra" — grants access via *either* `CompanyMembership` (company staff) *or* an active `SiteMembership` (client, architect, engineer, foreman, service provider attached only to that specific site, not to the company). A site-only member could therefore move/create/delete cards (authorized via `SiteAccessService`) but never receive the resulting SSE events (their subscription's company-id set was empty). Fixed by resolving the SSE scope the same dual way: `CompanyMembership.companyId` union `ConstructionSite.companyId` for every site the user has an active `SiteMembership` on.

Alternative considered: change `SiteAccessService` itself to also grant a `CompanyMembership`-like record. Rejected — conflates two intentionally distinct concepts (internal staff vs. per-site external team) for the sake of one subscriber-resolution query; resolving it locally in `SseController` is a three-line, self-contained fix.

### 2. Isolate the predefined-label-catalog fetch inside `TasksBoardPanel.load()`
`GET /api/companies/{companyId}/task-labels` (`CompanyTaskLabelService.list`) is intentionally gated to company staff (`CompanyMembership`) only — it's the shared, company-wide label catalog, a company-admin-configured resource, not a site-scoped one. A site-only member legitimately gets HTTP 403 there. The bug was structural, not authorization-related: `load()` had no `try/catch` around that one fetch, so its rejection propagated out of `load()` into whichever action handler called it (`onDrop`, `onCreateCard`, ...), which then reported the *whole action* as failed even though the move/create/etc. had already succeeded. Fixed by giving that fetch its own `try/catch`, defaulting to an empty label list on failure — a site-only member simply doesn't get the "attach an existing predefined label" picker populated (they can still create card-only custom labels, and still see labels already attached via the board response, which is site-scoped, not company-staff-gated).

Alternative considered: relax `CompanyTaskLabelService.list`'s authorization to accept site-only members too. Rejected — changes who can read the company-wide label catalog, a broader authorization decision belonging to `company-task-labels`, not something to fold into a board-loading bug fix.

### 3. One generic `task-card-updated` event, not one event per mutation type
Due date, assignee add/remove, predefined-label attach/detach, custom-label creation, and comment-added are five different service methods across three services (`TaskCardService`, `TaskLabelService`, `TaskCommentService`). Rather than five bespoke events, all of them publish the same `task-card-updated` shape — the card's full current derived state (due date, labelIds + full label objects, assigneeIds, commentCount) — via one shared `TaskCardService.publishCardUpdated(cardId)`, injected into the other two services. The frontend applies it as a single merge into the matching card object, regardless of which underlying field(s) actually changed.

Alternative considered: per-mutation events (`task-card-label-attached`, `task-card-comment-added`, ...) carrying only the delta. Rejected as premature precision — the frontend already re-renders the whole card cheaply either way (it's a small object), and one shared publish path is far less code than five, at the cost of a slightly larger-than-strictly-needed payload per event (acceptable at this event volume).

### 4. `task-card-updated` carries full label objects, not just ids
A recipient's local `board.value.labels` / `predefinedLabels.value` catalogs may not include a label that just got created or attached — most notably a brand-new card-only custom label (no other client has ever seen it), but also a predefined label for a site-only member whose `predefinedLabels.value` is `[]` per Decision 2. Including each attached label's `{id, companyId, cardId, name, colorHex}` directly in the event (mirroring `TaskLabelResponse`) means the receiving board can always render the label pill correctly without a follow-up fetch.

## Risks / Trade-offs

- **[Risk] `publishCardUpdated` re-queries labels/assignees/comment-count from the database on every single-field edit (e.g. just a due-date change).** → Mitigation: acceptable at Tasks-board interaction volume (human-driven edits, not bulk/programmatic); matches the same "small extra queries for real-time correctness" trade-off already accepted for `task-card-moved`/`task-card-created`.
- **[Risk] `TaskLabelService` and `TaskCommentService` now depend on `TaskCardService`.** → Mitigation: one-directional (`TaskCardService` has no reverse dependency on either), so no circular-bean risk; verified by the full Spring context test passing.
- **[Trade-off] No live comment thread.** → Accepted per Non-Goals; the comment count updating live is enough signal for "something happened here, go look."

## Migration Plan

No migration needed — additive SSE event types plus two bug fixes, all backward-compatible with the already-archived `add-task-card-sse-events` change. Already implemented, tested (backend suite + frontend build), and verified in the working session that produced this proposal; nothing further to roll out beyond the normal commit/deploy the user handles themselves.
