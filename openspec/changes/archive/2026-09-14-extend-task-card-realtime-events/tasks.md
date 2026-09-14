## 1. Bug fixes found during multi-session testing

- [x] 1.1 `SseController.subscribe()`: resolve company scope via `SiteMembership` (mapped to each site's `companyId`) in addition to `CompanyMembership`, so a site-only member receives their company's events.
- [x] 1.2 `TasksBoardPanel.vue` `load()`: isolate the `listCompanyLabels(companyId)` call in its own try/catch (default to `[]` on failure) so a site-only member's rejected label-catalog fetch doesn't fail the whole load or falsely mark a successful action as failed.

## 2. `task-card-created` and `task-card-deleted` events

- [x] 2.1 `TaskCardService.createCard()`: publish `task-card-created` (company-scoped) after save, with the new card's full data.
- [x] 2.2 `TaskCardService.deleteCard()`: publish `task-card-deleted` (company-scoped) after delete, with `cardId` + `constructionSiteId`.
- [x] 2.3 `TasksBoardPanel.vue`: subscribe to both events; on `created`, push the new card into local state (guarding against a duplicate if the actor's own `load()` already added it); on `deleted`, remove the card and close its detail modal if it was open.

## 3. `task-card-updated` event for in-place edits

- [x] 3.1 Add `TaskCardService.publishCardUpdated(UUID cardId)`: resolves the card + site + current labels/assignees/comment-count and publishes `task-card-updated` (company-scoped), including full label objects (not just ids).
- [x] 3.2 Call it from `TaskCardService.updateDueDate()`, `assign()`, and `unassign()`.
- [x] 3.3 Inject `TaskCardService` into `TaskLabelService`; call `publishCardUpdated(cardId)` from `createCustom()`, `attachPredefined()`, and `detach()`.
- [x] 3.4 Inject `TaskCardService` into `TaskCommentService`; call `publishCardUpdated(cardId)` from `add()`.
- [x] 3.5 `TasksBoardPanel.vue`: subscribe to `task-card-updated`; merge the event's fields into the matching card (due date, labelIds, assigneeIds, commentCount) and upsert any label objects it carries into `board.value.labels`.

## 4. Verification

- [x] 4.1 Backend: `cd pantheon-service && ./mvnw test` — full suite green, including updated `SseController`-dependent behavior and new/updated tests in `TaskCardServiceTest`, `TaskLabelServiceTest`, `TaskCommentServiceTest` verifying each new publish call.
- [x] 4.2 Frontend: `cd pantheon-web && npm run build` (vue-tsc + vite build) green after each round of `TasksBoardPanel.vue` changes.
- [ ] 4.3 Manual multi-account re-test (site-only member + company-staff member, same company): confirm `task-card-moved` now reaches the site-only account, confirm the false error message is gone, and confirm create/delete/update all sync live between the two sessions. **Not run in this session** — left for the user, same as the original change's manual-verification task.
