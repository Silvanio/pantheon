## Why

The Tasks board card face is too thin to work from: there's no expected completion date, labels are just unlabeled color slivers, and the only way to read or post a comment is to open the detail modal. Members scanning the board can't tell what's due or when, and discussing a card requires leaving the board context.

## What Changes

- `TaskCard` gains an optional `dueDate` ("data de conclusão prevista") settable at card creation and editable afterward; the card face shows it as a formatted date.
- The card face renders labels as named, colored pills (replacing today's unlabeled color slivers) instead of only showing them in the detail modal.
- Each card gets a collapsed-by-default expandable section at the bottom showing a comment count; expanding it lists that card's comments and lets the user post a new one inline, without opening the detail modal. The detail modal keeps handling title/label management; its own comments section is removed since the inline panel replaces it.

## Capabilities

### Modified Capabilities
- `obra-tasks-board`: `TaskCard` gains an optional due date (settable and editable, displayed on the card); card-face rendering must show labels and support reading/adding comments inline without the detail modal.

## Impact

- **pantheon-service**: `TaskCard` entity + migration gain `due_date`; `TaskCardCreationRequest`/`TaskCardResponse` carry it; a new endpoint to update a card's due date; `TaskCardService`/`TaskCardServiceTest` updated.
- **pantheon-web**: `useTaskCards.ts` (due date on create/update, typed on `TaskCard`), `TasksBoardPanel.vue` (label pills on card face, due date display, per-card expandable inline comments, modal simplified), `pt-BR.json` additions.
