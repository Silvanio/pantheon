## 1. Data model: rescope labels, add assignees (pantheon-service)

- [x] 1.1 Flyway `V49__rescope_task_label.sql`: `DELETE FROM task_card_label; DELETE FROM task_label;` (disposable dev-only rows, per precedent); `ALTER TABLE task_label DROP COLUMN construction_site_id`, add `company_id UUID REFERENCES company (id)` and `card_id UUID REFERENCES task_card (id)`, both nullable, plus `CHECK ((company_id IS NOT NULL AND card_id IS NULL) OR (company_id IS NULL AND card_id IS NOT NULL))`; indexes on `company_id` and `card_id`; then `INSERT INTO task_label (id, company_id, name, color_hex, created_at) SELECT gen_random_uuid(), c.id, 'Urgente', '#EF4444', now() FROM company c` to seed every existing company
- [x] 1.2 Flyway `V50__create_task_card_assignee_table.sql`: `task_card_assignee(id, card_id UUID NOT NULL REFERENCES task_card, site_membership_id UUID NOT NULL REFERENCES site_membership, created_at)`, unique index on `(card_id, site_membership_id)`, index on `card_id`
- [x] 1.3 `TaskLabel` entity: replace `constructionSiteId` with nullable `companyId` and nullable `cardId`; update constructor, getters (`getCompanyId()`, `getCardId()`); add `isPredefined()`/`isCustom()` helpers if useful
- [x] 1.4 New `TaskCardAssignee` entity (`id`, `cardId`, `siteMembershipId`, `createdAt`) + `TaskCardAssigneeRepository` (`findByCardIdIn(List<UUID>)`, `findByCardIdAndSiteMembershipId`, `deleteByCardId`)
- [x] 1.5 `TaskLabelRepository`: replace `findByConstructionSiteId*` with `findByCompanyIdOrderByNameAsc(UUID companyId)` and `findByCardId(UUID cardId)`; keep `findByCardIdIn`-style bulk lookups if still needed elsewhere (check `GlobalTaskBoardService` usage from the prior change and update it to `findByCompanyIdIn`)
- [x] 1.6 Exceptions: none new expected (reuse `TaskLabelNotFoundException`); add `TaskCardAssigneeNotFoundException` only if a lookup-by-id path needs it (attach/detach can no-op like label attach does)

## 2. Backend: predefined label catalog (company-admin, new `company-task-labels`)

- [x] 2.1 New `CompanyTaskLabelService` (or extend `TaskLabelService` with a clearly separated admin-only section) mirroring `TaskColumnService`'s `requireAdmin(companyId, userId)` pattern: `list(companyId, actingUserId)` (any active company member), `create(companyId, actingUserId, TaskLabelRequest)`, `delete(labelId, actingUserId)` — deleting a predefined label also removes any `TaskCardLabel` rows referencing it
- [x] 2.2 `CompanyService.create`: after saving the `Company` and `ADMIN` `CompanyMembership`, seed a predefined `TaskLabel(companyId=company.getId(), name="Urgente", colorHex="#EF4444")` via `TaskLabelRepository`
- [x] 2.3 New `CompanyTaskLabelController`: `GET/POST /api/companies/{companyId}/task-labels`, `DELETE /api/companies/{companyId}/task-labels/{labelId}`
- [x] 2.4 `ConstructionExceptionHandler`: confirm existing `TaskLabelNotFoundException`/`NotCompanyAdminException` handlers already cover the new endpoints (add nothing if so)

## 3. Backend: card-scoped custom labels, attach/detach, comment count, assignees, deletion

- [x] 3.1 `TaskLabelService` (card-facing half): `createCustom(cardId, actingUserId, TaskLabelRequest)` — `requireManage` on the card's site, persists `TaskLabel(cardId=cardId, ...)`, and in the same transaction inserts the matching `TaskCardLabel` row (auto-attached, no separate attach call); `attachPredefined(cardId, labelId, actingUserId)` validates the label's `companyId` matches the card's site's company (via `ConstructionSiteRepository`) instead of the old site check; `detach(cardId, labelId, actingUserId)` unchanged in shape
- [x] 3.2 `TaskCardService`: add `commentCountByCard(List<UUID> cardIds)` aggregation (new `TaskCommentRepository` method, e.g. `@Query("select c.cardId, count(c) from TaskComment c where c.cardId in :cardIds group by c.cardId")` returning `List<Object[]>` or a projection), folded into `TaskBoard` as `commentCountByCard: Map<UUID, Long>`; same pattern for `assigneeIdsByCard: Map<UUID, List<UUID>>` (site membership ids) via `TaskCardAssigneeRepository.findByCardIdIn`
- [x] 3.3 `TaskCardService.assign(cardId, actingUserId, siteMembershipId)` / `unassign(...)`: `requireManage` on the card's site, validate the membership belongs to the same `constructionSiteId` as the card, insert/delete `TaskCardAssignee`
- [x] 3.4 `TaskCardService.deleteCard(cardId, actingUserId)`: `requireManage`, then delete in order — `TaskCardLabelRepository` rows for the card, `TaskLabelRepository` rows where `cardId` matches (the card's own custom labels), `TaskCommentRepository` rows for the card, `TaskCardAssigneeRepository` rows for the card, finally the `TaskCard` itself
- [x] 3.5 `TaskCardResponse`: add `commentCount` (long/int) and `assigneeIds` (`List<UUID>` of site membership ids); `TaskCardController`: populate both from the board's maps (mirroring `labelIds`); add `DELETE /api/task-cards/{cardId}`, `POST/DELETE /api/task-cards/{cardId}/assignees/{siteMembershipId}`, `POST /api/task-cards/{cardId}/custom-labels` (custom-label creation, distinct from the existing `POST /api/task-cards/{cardId}/labels/{labelId}` attach-by-id endpoint)
- [x] 3.6 `TaskLabelResponse`: add `companyId`/`cardId` (nullable) reflecting the new scope, drop `constructionSiteId`

## 4. Backend tests

- [x] 4.1 New `CompanyTaskLabelServiceTest` (or extend `TaskLabelServiceTest`): admin-only create/delete enforcement, non-admin rejected, member (non-admin) can still list
- [x] 4.2 `CompanyServiceTest` (or wherever company creation is tested): creating a company yields a predefined "Urgente" label for it
- [x] 4.3 `TaskLabelServiceTest`: `createCustom` persists a card-scoped label and auto-attaches it in one call; a custom label from one card never appears when listing another card's/company's predefined catalog; `attachPredefined` rejects a label from a different company
- [x] 4.4 `TaskCardServiceTest`: `commentCountByCard`/board response reflects actual comment counts with no N+1 (assert repository called once for a batch, not per card, if easily assertable — otherwise just assert correct counts); `assign`/`unassign` persist and remove `TaskCardAssignee`, reject a membership from a different site; `deleteCard` removes the card and all dependent rows, rejects a caller without `MANAGE`
- [x] 4.5 `GlobalTaskBoardServiceTest`: update for the `TaskLabelRepository` method rename (`findByCompanyIdIn` instead of `findByConstructionSiteIdIn`)

## 5. pantheon-web: composables

- [x] 5.1 New `useTaskLabels.ts` (or extend `useTaskCards.ts`) with `listCompanyLabels(companyId)`, `createCompanyLabel(companyId, name, colorHex)`, `deleteCompanyLabel(companyId, labelId)` for the predefined catalog (Company Settings)
- [x] 5.2 `useTaskCards.ts`: `TaskCard` interface gains `commentCount: number` and `assigneeIds: string[]`; `TaskLabel` interface gains `companyId: string | null` / `cardId: string | null` (or drop the old `constructionSiteId` field it currently has); `listLabels(siteId)` → `listAttachableLabels(companyId)` hitting the new predefined-catalog endpoint (or reuse `useTaskLabels`'s `listCompanyLabels`); new `createCustomLabel(cardId, name, colorHex)`, `assignMember(cardId, siteMembershipId)`, `unassignMember(cardId, siteMembershipId)`, `deleteCard(cardId)`

## 6. pantheon-web: card face rework

- [x] 6.1 `TasksBoardPanel.vue`: remove `expandedCardId`, `commentsByCard`, `inlineCommentDrafts`, `postingInlineCommentFor` state and the inline comment-expander markup entirely
- [x] 6.2 Card face becomes `position: relative`; due date renders as a small discreet marker `absolute top-1.5 right-2 text-[10px]` (drop the calendar-emoji-plus-full-line treatment), keeping the overdue/due-today color distinction
- [x] 6.3 Label pills shrink to a visibly minor size (e.g. `px-1.5 py-0.5 text-[9px]`, down from the current `px-2.5 py-1 text-xs`)
- [x] 6.4 Small comment-count icon (speech-bubble SVG + number, shown only when `commentCount > 0` or always shown at `0` per whichever reads cleaner — prefer hiding at 0 to stay discreet) placed in the card footer; no click handler of its own (clicking anywhere on the card, including this icon, calls the existing `openCard`)
- [x] 6.5 Small assignee avatar-initials cluster (derived from each assigned site member's `displayName`/`email`) in the card footer next to the comment icon

## 7. pantheon-web: modal rework (opaque surface, comments, custom labels, assignees, delete)

- [x] 7.1 Fix the transparency bug: stop reusing `.card` for the modal's dialog surface; add a `.modal-panel` class to `style.css` (`rounded-2xl border border-steel-200 bg-white shadow-sm dark:border-steel-800 dark:bg-steel-900` — fully opaque in both themes) and use it for this modal (and check whether other existing modals in the codebase have the same bug worth flagging, without fixing them if out of scope)
- [x] 7.2 Modal regains a comments section (list + add form), reusing the same `listComments`/`addComment` calls the inline expander used before — this is now the only place comments are readable/postable
- [x] 7.3 Modal's label section: predefined labels (fetched via the new company-catalog endpoint) toggle attach/detach as today; add a "create custom label" mini-form (name + color swatches, matching `LABEL_COLORS`) that calls `createCustomLabel` and immediately reflects the label as attached (no separate toggle step)
- [x] 7.4 Modal gains an "Assign members" section: checklist of the site's members (reuse `useSiteMembers`/`SiteMember` — key on `membershipId`), toggling calls `assignMember`/`unassignMember`
- [x] 7.5 Modal gains a delete button (confirm-before-destructive per this app's existing conventions if any exist elsewhere, otherwise a plain button is consistent with current card actions) calling `deleteCard`, closing the modal and refreshing the board on success

## 8. pantheon-web: Company Settings predefined-labels panel

- [x] 8.1 New `CompanyTaskLabelsPanel.vue` mirroring `CompanyTaskColumnsPanel.vue`'s shape exactly (admin-only gating via `listMyCompanies()`, list + create + delete controls, no reorder)
- [x] 8.2 Mount `<CompanyTaskLabelsPanel :company-id="companyId" />` in `CompanySettingsView.vue` right after `<CompanyTaskColumnsPanel />`

## 9. i18n

- [x] 9.1 `pt-BR.json` `tasks.*`: remove now-unused inline-expander strings if any become dead, add `deleteCard`, `deleteCardConfirm` (if a confirm step is added), `assignees`, `assignMember`/`unassignMember` labels, `customLabelButton`/`customLabelPlaceholder`; add `company.taskLabels.*` for the new Company Settings panel (mirroring `company.taskColumns.*`)

## 10. Verification

- [x] 10.1 `pantheon-service`: `mvn -q -o compile`, `mvn -q -o test-compile`, and `mvn -q -o test` all pass (real Postgres + Flyway V49–V50 applied cleanly)
- [x] 10.2 `pantheon-web`: `vue-tsc -b --force` and `vite build` both pass clean
- [x] 10.3 Manual QA in a live browser: confirm every existing company (including the QA company from prior manual testing) already has an "Urgente" predefined label after the migration; create a new company and confirm it also gets "Urgente" automatically; as admin, add/delete a predefined label in Company Settings; on a card, attach a predefined label, create a custom label (confirm it's auto-attached and never appears on another card), confirm label pills are visibly smaller; confirm due date shows discreetly in the top-right corner; confirm the card face shows a small comment-count icon with no "Comentários" text/chevron, and that clicking anywhere on the card (including that icon) opens an now-opaque modal with no board visible through it; post a comment from inside the modal and confirm a closed card offers no way to comment; assign two members to a card and confirm their initials show on the card face; delete a card and confirm it disappears from the board
- [x] 10.4 Update this file's checkboxes to reflect actual completion as work proceeds
- [ ] 10.5 Run `openspec archive` once implemented and verified, updating `openspec/specs/` accordingly
