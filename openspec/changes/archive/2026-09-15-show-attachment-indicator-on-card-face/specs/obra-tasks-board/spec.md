## MODIFIED Requirements

### Requirement: Real-time sync of in-place card edits
`pantheon-service` SHALL emit a real-time event, scoped to the card's company, whenever an existing card is edited in place — its due date, its assigned site members, its attached or created labels, its comment count, or its attachment count — so that other members of that company with the board or that card's detail modal open see the change without reloading. The event SHALL include enough information (including full data for any label the edit newly attaches) to render the change without a follow-up fetch.

#### Scenario: Due date change seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` sets or clears a card's due date
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's new due date without a manual reload

#### Scenario: Assignee change seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` assigns or unassigns a site member on a card
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's updated assignees without a manual reload

#### Scenario: Label change seen in real time, including a brand-new custom label
- **WHEN** a member with `MANAGE` access to `TASKS` attaches or detaches a predefined label, or creates a card-only custom label
- **THEN** `pantheon-web` clients belonging to the same company show the card's updated labels, correctly named and colored, without a manual reload and without needing to have independently fetched that label

#### Scenario: Comment count seen in real time
- **WHEN** a member with `MANAGE` access to `TASKS` posts a comment on a card
- **THEN** `pantheon-web` clients belonging to the same company see the card's comment count increase without a manual reload; the comment's own text is not pushed and still requires opening the card to read

#### Scenario: Attachment count seen in real time
- **WHEN** a file gains or loses a link to a card — attached from the card's detail modal, deleted directly from Projetos, or removed via a folder's cascading delete — the card's attachment count changes
- **THEN** `pantheon-web` clients belonging to the same company reflect the card's updated attachment count without a manual reload

## ADDED Requirements

### Requirement: Attachment indicator on the card face
`pantheon-web` SHALL show a small, discreet icon on a card's face, next to the comment-count icon, whenever that card has at least one attached file; the icon SHALL NOT be shown when the card has no attachments. Attaching a file from the card's own detail modal SHALL make the icon appear immediately for the member who attached it, without waiting on a real-time round trip.

#### Scenario: Card face shows the attachment icon
- **WHEN** a card has one or more files attached to it
- **THEN** `pantheon-web` shows a small attachment icon on that card's face, next to the comment-count icon

#### Scenario: Card face has no attachment icon when there are no attachments
- **WHEN** a card has no files attached to it
- **THEN** `pantheon-web` does not show an attachment icon on that card's face

#### Scenario: Icon appears immediately after attaching from the modal
- **WHEN** a member with `MANAGE` access to `TASKS` attaches a file to a card from its detail modal
- **THEN** `pantheon-web` shows the attachment icon on that card's face without requiring a manual reload
