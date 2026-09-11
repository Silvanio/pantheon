## Context

The Tasks board (`obra-tasks-board`) shipped in two prior changes: base board (columns/cards/labels/comments) then due date + inline comment expansion + visible labels. Real use immediately surfaced problems with the second round's choices (inline comments too loud, labels too big) and gaps the first round left open (no assignees, no delete, labels stuck at obra scope, a modal-transparency bug). This round reworks the card face/modal and rescopes labels.

Current shape being changed:
- `TaskLabel(id, constructionSiteId, name, colorHex, createdAt)` — one flat scope, obra-level.
- `TaskCardService.getBoard` returns `TaskBoard(columns, cards, labelIdsByCard)`; `TaskCardResponse` carries `labelIds` but no comment count or assignees.
- `TasksBoardPanel.vue`'s card face has an inline `expandedCardId`/`commentsByCard` comment expander (being removed) and label pills sized for legibility rather than discretion.
- The detail modal is `<div class="card card-pad ...">`; `.card` is `border-steel-200 bg-white shadow-sm dark:border-steel-800 dark:bg-steel-800/60` — the `/60` alpha is the transparency bug.
- `TaskColumnService` is the existing template for a company-admin-gated, company-scoped resource (`requireAdmin(companyId, userId)` via `CompanyMembershipRepository`, throwing `NotCompanyAdminException`).
- `CompanyService.create` today only saves a `Company` row and one `ADMIN` `CompanyMembership` — no precedent yet for seeding other rows on company creation, so this change introduces that pattern for the first time.

## Goals / Non-Goals

**Goals:**
- Quieter card face: small comment-count icon, corner due date, small label pills, small assignee avatars — none competing with the title.
- Comments readable/postable only through the modal (single place, simpler than syncing two surfaces).
- Two-tier labels: reusable company catalog (admin-managed) vs. disposable card-only custom labels, auto-attached on creation.
- "Urgente" always available, for every company, without admin setup.
- Assign site members to a card; delete a card.
- Fix the modal opacity bug.

**Non-Goals:**
- No reordering/drag-and-drop for label catalog entries (mirror `TaskColumn`'s simple list + rename/delete, no reorder requirement was raised for labels).
- No notification/activity feed for assignment or comments.
- No permission tier finer than the existing `TASKS` `VIEW`/`MANAGE`; assigning/deleting/commenting all gate on the same `MANAGE` check already used for create/move.
- No editing of a custom label's name/color after creation (it's created once, attached once; delete-and-recreate covers the rare correction).

## Decisions

- **Label scope: `companyId` XOR `cardId` on the same `TaskLabel` table**, not two separate tables. A predefined label has `companyId` set and `cardId` null; a custom label has `cardId` set and `companyId` null. One entity, one repository, one `TaskCardLabel` join table keeps attach/detach code (`TaskCardLabelRepository.findByCardIdIn`, the unique `(card_id, label_id)` index) unchanged. The predefined catalog query (`findByCompanyIdOrderByNameAsc`) naturally excludes custom labels (their `companyId` is null), and a custom label is never returned to any card other than the one it was created for because it's simply never queried by `companyId` — it only ever reaches the UI via that one card's `labelIds`/join rows.
- **Migration clears existing `task_label`/`task_card_label` rows** rather than backfilling a `companyId` from each label's old `constructionSiteId`. Precedent: the already-archived `add-suppliers-and-purchase-request-headers` change's `V40` did the same for disposable dev-only rows before adding new `NOT NULL` columns. This is pre-launch data with no business value.
- **"Urgente" seeding happens in two places**: `CompanyService.create` inserts it (via `TaskLabelRepository`) right after saving the new `Company`/`ADMIN` `CompanyMembership`, in the same transaction; the rescoping migration inserts one `Urgente` row per existing `company` row directly in SQL (`INSERT ... SELECT id, ... FROM company`), since by the time that statement runs the table has already been cleared by the same migration — no conflict with the seeded rows for companies that already exist.
- **Predefined-label CRUD is company-admin-gated, mirroring `TaskColumnService`** exactly: a new `requireAdmin(companyId, userId)` (or reuse `TaskColumnService`'s if it's extracted into a shared helper — otherwise duplicate the small check, matching the codebase's existing per-service duplication of this pattern rather than introducing a new shared abstraction for two callers). Custom-label creation (from inside a card) stays gated the way label attach/detach already is: `SiteAccessService.requireAccess` + `SitePermissionService.requireManage(siteId, access, PermissionCapability.TASKS)`.
- **`TaskCardAssignee` keyed by `membershipId`, not `userId`.** `SiteMember.userId` is nullable (service-provider members without a registered account), so keying by `userId` would make a whole class of real team members unassignable. `TaskCardAssignee(id, cardId, membershipId, createdAt)`, FK to the site-membership table (`ConstructionSiteMembership` — confirm exact class name while implementing; the codebase's own `useSiteMembers.ts` already treats `membershipId` as the stable identifier for exactly this reason).
- **Comment count is aggregated in `TaskCardService.getBoard`**, mirroring `labelIdsByCard`: a new `TaskCommentRepository` method grouping `COUNT(*)` by `cardId` for a batch of card ids, folded into `TaskBoard` as `commentCountByCard: Map<UUID, Long>`, then into `TaskCardResponse.commentCount`. Avoids N+1 and avoids the previous round's per-card lazy-fetch-on-expand approach, which no longer applies now that the board never shows comment bodies inline.
- **Modal surface**: introduce a dedicated opaque class (e.g. `.modal-panel` in `style.css`, `bg-white dark:bg-steel-900` — the same opaque tone the card faces themselves already use) instead of reusing `.card`'s translucent dark variant. Scoped to this modal; not a global rename of `.card` (other ambient uses of `.card` may rely on the translucency intentionally, and auditing every call site is out of scope here).
- **Card deletion cleans up dependents in the service layer**, not via DB cascade: delete `TaskCardLabel` rows for the card, delete `TaskLabel` rows where `cardId` matches (the card's own custom labels), delete `TaskComment` rows for the card, delete `TaskCardAssignee` rows for the card, then delete the `TaskCard` — explicit and consistent with the codebase's existing style of no `ON DELETE CASCADE` elsewhere in the Tasks board schema.
- **New-custom-label-auto-attached-by-default**: creating a custom label from the modal is a single action (name + color) that both inserts the `TaskLabel(cardId=...)` row and inserts the matching `TaskCardLabel` row in the same request/transaction — no separate "attach" step, unlike predefined labels which still use the existing toggle-to-attach interaction.

## Risks / Trade-offs

- [Breaking label rescope loses any labels/attachments a real user created during the last two rounds' manual QA] → Acceptable: this app has no production users yet (confirmed by the identical precedent already taken for purchase-request/orçamento dev rows), and the migration is explicit about clearing them.
- [Duplicating the `requireAdmin(companyId, userId)` check between `TaskColumnService` and the new predefined-label service] → Matches the codebase's current convention (each service already repeats this shape rather than sharing a base class); introducing a shared helper is a larger refactor than this change's scope warrants.
- [`TaskCardAssignee` referencing a site-membership row rather than a user means a removed/deactivated member's assignment lingers as a dangling-looking row] → Same lifecycle behavior the codebase already accepts elsewhere for membership references; not a new class of risk introduced by this change.

## Open Questions

None — scope and every ambiguous point (label scope model, assignee key, comment-count aggregation, modal fix approach, deletion cleanup) resolved above before implementation.
