## Why

The just-shipped Tasks board card face is too loud and too limited from real use: the inline "Comentários" expander eats space with text and an oversized toggle, label pills are large enough to dominate the card, the due date competes with the title for attention, there's no way to say who a card is for, no way to remove a card once created, and the labels a member types while filling out a card can only ever be created scoped to a single obra with no reusable company-wide catalog. Separately, opening a card's detail view shows the board bleeding through it — a real CSS bug — because the dialog reuses a translucent ambient panel style instead of an opaque one.

## What Changes

- The inline expandable comment section is removed. Each card face shows a small, discreet comment-count icon instead of the "Comentários" text and chevron; comments are only ever read or posted from inside the detail modal — a closed card never accepts a comment.
- The due date moves to a small, discreet marker in the card face's top-right corner instead of its own line of text.
- Label pills on the card face shrink to a visually minor size (they currently read as loud as the title).
- **BREAKING**: `TaskLabel` is rescoped away from `constructionSiteId`. Two kinds of label now exist: company-scoped **predefined** labels (managed by the company admin from Company Settings, the same place columns are configured, reusable across every obra of that company) and card-only **custom** labels (created directly inside a card's detail modal, existing only on that one card, auto-attached to it the moment they're created). Every company gets an "Urgente" predefined label automatically — seeded for new companies at creation time and backfilled for existing companies. Existing `task_label`/`task_card_label` rows are disposable dev-only data (same precedent as the already-archived purchase-request-headers migration) and are cleared as part of the rescoping migration.
- New: one or more site members can be assigned to a card, shown as small avatar-initials on the card face and managed from a checklist in the modal.
- Fixed: the card detail modal now renders on an opaque surface — today it reuses the shared `.card` class's translucent dark-mode background, letting the board show through it.
- New: a card can be deleted (same `TASKS` `MANAGE` gate as creating/moving one), cleaning up its comments, its own custom labels, its label attachments, and its assignees.

## Capabilities

### New Capabilities
- `company-task-labels`: company-admin-managed catalog of predefined, reusable Tasks-board labels, configured from Company Settings alongside task columns. Every company has an "Urgente" predefined label from the start.

### Modified Capabilities
- `obra-tasks-board`: card face and detail-modal UX reworked (comment count icon replacing inline expansion, due date repositioned, smaller label pills, opaque modal); labels attached to a card can now be either a company-predefined label or a card-only custom label (auto-attached on creation); cards gain assignable site members and can be deleted.

## Impact

- **pantheon-service**: `TaskLabel` entity/migration rescoped (`companyId` XOR `cardId`, breaking); new `TaskCardAssignee` entity/migration/repository; `TaskLabelService` split into company-admin catalog CRUD vs. card-scoped custom-label creation; `TaskCardService` gains `deleteCard`, assignee management, and an aggregated comment-count-per-card query (mirroring the existing `labelIdsByCard` pattern); `CompanyService.create` seeds the default "Urgente" label; new `TaskColumnController`-sibling endpoints for the predefined label catalog and for assignees; `TaskCardResponse`/`TaskBoardResponse` gain `commentCount` and assignee fields.
- **pantheon-web**: `TasksBoardPanel.vue` card face and modal reworked (icon-based comment count, top-right due date, smaller label pills, assignee avatars/checklist, delete button, opaque modal surface); new `CompanyTaskLabelsPanel.vue` mounted in `CompanySettingsView.vue` next to `CompanyTaskColumnsPanel.vue`; `useTaskCards.ts` and a new `useTaskLabels.ts`/assignee composable updated; `pt-BR.json` additions.
