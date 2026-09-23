## ADDED Requirements

### Requirement: Schedule structure (stages and tasks)
`pantheon-service` SHALL let a construction site have a schedule composed of ordered "Etapas" (stages), each with a name, color, start date, and end date, and each containing ordered "Tarefas" (tasks), each with a title, start date, end date, an optional responsible `SiteMembership`, and a completion percentage (`0`–`100`). A stage's own displayed completion percentage SHALL be the average of its tasks' completion percentages (`0` when it has no tasks), never a separately stored value.

#### Scenario: Creating a stage with tasks
- **WHEN** a member with `MANAGE` access to `SCHEDULE` creates a stage and adds two tasks to it, one at `40%` and one at `60%` complete
- **THEN** `pantheon-service` reports that stage's completion as `50%`

#### Scenario: Empty stage has zero completion
- **WHEN** a stage exists with no tasks yet
- **THEN** `pantheon-service` reports that stage's completion as `0%`

### Requirement: Schedule visible on the obra's Cronograma tab, gated by SCHEDULE capability
`pantheon-web` SHALL show a construction site's schedule as a Gantt chart on its "Cronograma" tab, visible to any member whose resolved `SCHEDULE` access is `VIEW` or `MANAGE`, and hidden (both the tab and the underlying data) from a member whose resolved `SCHEDULE` access is `HIDDEN`. Creating, editing, deleting, or reordering a stage or task, and linking or unlinking a dependency, SHALL require `MANAGE`; a `VIEW` member SHALL see the chart read-only.

#### Scenario: View-only member sees the chart without edit controls
- **WHEN** a member whose resolved `SCHEDULE` access is `VIEW` opens the Cronograma tab
- **THEN** `pantheon-web` renders the Gantt chart but hides every create/edit/delete control, and `pantheon-service` rejects a write attempt with HTTP 403

#### Scenario: Hidden member has no Cronograma tab
- **WHEN** a member whose resolved `SCHEDULE` access is `HIDDEN` opens the obra
- **THEN** `pantheon-web` does not show the Cronograma tab, and `pantheon-service` rejects a direct request for that site's schedule with HTTP 403

### Requirement: Task dependency links are visual only
`pantheon-service` SHALL allow a task to be linked to another task on the same construction site as a predecessor/successor dependency, rejecting a link to itself or a duplicate of an existing link. `pantheon-web` SHALL draw a connecting line between the two tasks' bars on the Gantt chart for each such link. Neither `pantheon-service` nor `pantheon-web` SHALL automatically change a task's dates as a result of a dependency or a linked task's date change.

#### Scenario: Linking two tasks draws a connector
- **WHEN** a member with `MANAGE` access links task A as a predecessor of task B
- **THEN** `pantheon-web` draws a line from task A's bar to task B's bar on the Gantt chart

#### Scenario: Changing a predecessor's dates does not move the successor
- **WHEN** task A (linked as a predecessor of task B) has its end date moved later
- **THEN** task B's start and end dates remain unchanged

#### Scenario: Self-dependency rejected
- **WHEN** a member attempts to link a task as its own predecessor
- **THEN** `pantheon-service` rejects the request with HTTP 400

### Requirement: Deleting a stage cascades to its tasks and dependencies
`pantheon-service` SHALL, when a stage with `MANAGE` access is deleted, also delete every task under that stage and every dependency link referencing one of those tasks. `pantheon-web` SHALL confirm this action before submitting it, naming how many tasks will be removed.

#### Scenario: Deleting a stage removes its tasks
- **WHEN** a member with `MANAGE` access deletes a stage that has three tasks
- **THEN** `pantheon-service` removes the stage and all three tasks, and any dependency links involving them

### Requirement: Obra progress is derived from its schedule, with a placeholder fallback
`pantheon-service` SHALL compute a construction site's overall progress percentage as the average completion percentage across every task in its schedule, and include it as a nullable `schedulePercentComplete` field on the construction-site responses `pantheon-web`'s dashboard consumes. When a site has no schedule tasks, this field SHALL be `null`. `pantheon-web`'s dashboard SHALL display `schedulePercentComplete` when it is not null, and otherwise fall back to its existing status-derived placeholder percentage, so a site with no schedule configured renders exactly as it did before this capability existed.

#### Scenario: Progress reflects real task completion
- **WHEN** a construction site has schedule tasks averaging `35%` complete
- **THEN** the dashboard shows that site's progress as `35%`, not the status-derived placeholder

#### Scenario: Site without a schedule keeps the placeholder
- **WHEN** a construction site has zero schedule tasks
- **THEN** `pantheon-service` returns `null` for `schedulePercentComplete`, and the dashboard shows its existing status-derived placeholder percentage unchanged
