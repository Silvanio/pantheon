## Why

Manual multi-session testing of `add-task-card-sse-events` (archived 2026-09-14) surfaced two real bugs and one scope gap: (1) a site-only team member (no `CompanyMembership`, only a `SiteMembership` on that obra) never received `task-card-moved` events at all, because SSE company-scope resolution only looked at `CompanyMembership`; (2) the Tasks board showed a spurious "Não foi possível concluir a ação" error after every successful action for that same population, because `TasksBoardPanel.load()` let an unrelated, stricter-scoped label-catalog fetch fail the whole load; (3) real-time sync only covered moving a card — creating, deleting, and editing (due date, assignees, labels, comments) still required a manual reload to see another user's change. This change fixes both bugs and extends real-time sync to the rest of the card lifecycle.

## What Changes

- Fix `SseController`'s company-scope resolution to also include companies reached via an active `SiteMembership` (not just `CompanyMembership`), matching the dual access model `SiteAccessService` already uses for every other site-scoped endpoint.
- Fix `TasksBoardPanel.load()` to isolate the company predefined-label-catalog fetch in its own try/catch (defaulting to an empty list on failure) instead of letting it fail the whole load and falsely mark an otherwise-successful action as failed.
- Add `task-card-created` and `task-card-deleted` events, published the same way as the existing `task-card-moved` (company-scoped, RabbitMQ fan-out).
- Add a `task-card-updated` event covering any in-place edit to an existing card: due date, assignee add/remove, predefined label attach/detach, custom label creation, and comment added (comment count only, not the comment body/thread). Centralized in a new `TaskCardService.publishCardUpdated(cardId)` reused by `TaskLabelService` and `TaskCommentService`.
- `task-card-updated`'s payload carries full label objects (not just ids) for any label currently attached to the card, so a label the recipient hasn't independently loaded (a card-only custom label, or a predefined label they can't fetch under `company-task-labels`' company-staff-only catalog endpoint) still renders correctly.
- `pantheon-web`'s `TasksBoardPanel.vue` now reacts to all four events (`task-card-moved`, `task-card-created`, `task-card-deleted`, `task-card-updated`), patching board state in place; a delete of the currently-open card closes its detail modal.

## Capabilities

### New Capabilities
(none — extends the real-time behavior introduced by `add-task-card-sse-events` rather than a new domain capability)

### Modified Capabilities
- `pantheon-service`: the "Server-Sent Events stream" requirement's company-scope resolution is corrected to include site-only members, not just company staff.
- `obra-tasks-board`: real-time sync extends from card movement only to creation, deletion, and in-place edits (due date, assignees, labels, comments); the board also no longer surfaces a false error when a site-only member's non-essential label-catalog fetch is rejected.
- `pantheon-web`: the SSE client's task-board reaction covers the three additional event types.

## Impact

- Affected code: `pantheon-service` (`controller/SseController.java`, `service/TaskCardService.java`, `service/TaskLabelService.java`, `service/TaskCommentService.java`), `pantheon-web` (`components/TasksBoardPanel.vue`).
- No new external dependencies, no database schema changes, no API contract changes (all changes are either bug fixes to existing behavior or new SSE event types additive to the existing stream).
- Implementation and backend/frontend verification were already completed in the same working session as this proposal (see tasks.md) — this change formalizes and archives that work rather than gating it behind a fresh apply pass.
