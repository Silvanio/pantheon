## ADDED Requirements

### Requirement: Creating a Tasks-board card from a schedule task
`pantheon-service` SHALL allow a member with `MANAGE` access to both `SCHEDULE` and `TASKS` on a construction site to create a new Tasks-board card from one of that site's schedule tasks, using the schedule task's title as the card's title and its end date as the card's due date, placed in the site's company's first task column (ordered by `sortOrder`). If the schedule task has a responsible site membership, that same site membership SHALL be assigned to the new card. The schedule task SHALL then carry that card's id. A schedule task that already carries a linked card SHALL reject a further attempt to create another one.

#### Scenario: Creating a linked card from a schedule task
- **WHEN** a member with `MANAGE` access to `SCHEDULE` and `TASKS` creates a Tasks-board card from a schedule task titled "Escavação" with end date 2026-10-05
- **THEN** `pantheon-service` creates a Tasks-board card titled "Escavação" with due date 2026-10-05 in the site's company's first task column, and the schedule task's response includes that card's id and title

#### Scenario: The task's responsible is carried over as the card's assignee
- **WHEN** a member creates a linked card from a schedule task that has a responsible site membership set
- **THEN** `pantheon-service` assigns that same site membership to the new Tasks-board card

#### Scenario: A task can only be linked once
- **WHEN** a member attempts to create a linked card from a schedule task that already has one
- **THEN** `pantheon-service` rejects the request with HTTP 409, and no second card is created

#### Scenario: Member without TASKS management cannot create a linked card
- **WHEN** a member whose resolved `TASKS` access is not `MANAGE` attempts to create a linked card from a schedule task
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Linked card shown on the schedule task
`pantheon-web` SHALL show, on a schedule task's edit panel, either a "Criar task" action (when the task has no linked card and the viewer can manage both `SCHEDULE` and `TASKS`) or the linked card's title with a way to open the Tasks board (once linked).

#### Scenario: Linked card title is shown
- **WHEN** a member opens the edit panel of a schedule task that already has a linked Tasks-board card
- **THEN** `pantheon-web` shows that card's title instead of the "Criar task" action

#### Scenario: Jumping to the Tasks board from a linked task
- **WHEN** a member clicks the linked card's title on a schedule task's edit panel
- **THEN** `pantheon-web` switches to the obra's Tasks tab
