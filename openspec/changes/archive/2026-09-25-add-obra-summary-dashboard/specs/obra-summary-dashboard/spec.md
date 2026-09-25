## ADDED Requirements

### Requirement: Aggregated obra summary endpoint
`pantheon-service` SHALL provide a single endpoint returning a construction site's summary — Cronograma progress, and for Diário de Obra/Pedido de Compra/Orçamentos/Equipamento/Projetos/Tasks/Equipe: a count plus (for Pedido de Compra, Orçamentos, Tasks, and Projetos) its 3 most recently created items. Each section SHALL be present only when the requesting member's resolved access to that section's capability is not `HIDDEN`; a `HIDDEN` section SHALL be omitted from the response entirely, not returned as an empty/zeroed section.

#### Scenario: Member with full access sees every section
- **WHEN** a member whose resolved access is not `HIDDEN` for any capability requests an obra's summary
- **THEN** `pantheon-service` returns all eight sections (schedule, daily reports, purchase requests, orçamentos, equipment, projects, tasks, team)

#### Scenario: Hidden capability omits its section
- **WHEN** a member whose resolved `EQUIPMENT` access is `HIDDEN` requests an obra's summary
- **THEN** the response contains no equipment section, while every other visible section is still present

#### Scenario: Recent items are newest-first
- **WHEN** an obra has more than 3 purchase requests
- **THEN** the purchase-request section's recent-items list contains exactly the 3 most recently created ones, newest first

### Requirement: Obra summary shown first on entering an obra
`pantheon-web` and `pantheon-mobile` SHALL show the obra summary — cards for each present section from the summary endpoint, plus a "Pendências" highlight combining the count of Pedido de Compra awaiting approval and draft Orçamentos — as the first thing shown when a member opens an obra, with all copy sourced from the `pt-BR` locale resource file (web) / hardcoded pt-BR strings (mobile, matching its existing convention). A recent item created within the last 7 days SHALL be visually flagged as new.

#### Scenario: Member opens an obra and sees the summary first
- **WHEN** a member opens an obra
- **THEN** the summary view (not any individual section like Equipe or Diário de Obra) is what they see first

#### Scenario: Pendências highlights actionable items
- **WHEN** an obra has 2 purchase requests awaiting approval and 1 draft orçamento
- **THEN** the Pendências highlight shows a combined count of 3

#### Scenario: Recently created item is flagged
- **WHEN** a purchase request was created 3 days ago
- **THEN** its entry in the recent-items list is visually marked as new

#### Scenario: Clicking a recent item opens its detail
- **WHEN** a member clicks a recent item in the summary (e.g. a recently created Orçamento)
- **THEN** the app navigates to that item's existing detail view
