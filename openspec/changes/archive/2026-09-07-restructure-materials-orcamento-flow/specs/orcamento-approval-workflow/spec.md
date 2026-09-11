## ADDED Requirements

### Requirement: Orçamento creation
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to create an `Orcamento` for that site, either empty or pre-filled with line items copied from selected `PurchaseRequestItem`s, starting in `DRAFT` (Rascunho) status.

#### Scenario: Orçamento created from scratch
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access creates a new Orçamento with no purchase-request items selected
- **THEN** `pantheon-service` persists a new `Orcamento` in `DRAFT` status with no line items

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Orçamento line item management
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to add, edit, or remove an `OrcamentoLineItem` (free-text name, optional type, quantity, and optional unit price) on an Orçamento only while it is in `DRAFT` status.

#### Scenario: Line item added while in Rascunho
- **WHEN** a construction site member submits a name, quantity, and unit price for a new line item on a `DRAFT` Orçamento
- **THEN** `pantheon-service` adds the line item to that Orçamento

#### Scenario: Line items cannot change once submitted
- **WHEN** a construction site member attempts to add, edit, or remove a line item on an Orçamento that is not `DRAFT`
- **THEN** `pantheon-service` rejects the request

### Requirement: Per-site Orçamento approval levels
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to configure an ordered list of Orçamento approval levels for that site, each specifying a step order and a required construction-site function. A site with no configured levels SHALL default to a single implicit level requiring the `ENGINEER` function.

#### Scenario: Admin configures a two-step chain
- **WHEN** a company staff member configures a site's approval levels as step 1 = `ENGINEER`, step 2 = `CLIENT`
- **THEN** `pantheon-service` persists both levels in that order for that site

#### Scenario: Unconfigured site defaults to a single level
- **WHEN** an Orçamento is submitted for approval on a site with no configured approval levels
- **THEN** `pantheon-service` creates a single approval step requiring the `ENGINEER` function

### Requirement: Submitting an Orçamento for approval
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to submit a `DRAFT` Orçamento for approval, transitioning it to `IN_APPROVAL` (Em aprovação) and creating one `OrcamentoApproval` record per configured approval level (or the default level), in order, all starting `PENDING`, tagged with a new approval cycle number.

#### Scenario: Orçamento submitted
- **WHEN** a construction site member submits a `DRAFT` Orçamento with at least one line item for approval
- **THEN** `pantheon-service` transitions it to `IN_APPROVAL` and creates its ordered approval steps for a new cycle

#### Scenario: Cannot submit an empty Orçamento
- **WHEN** a construction site member attempts to submit a `DRAFT` Orçamento with no line items
- **THEN** `pantheon-service` rejects the request

### Requirement: Acting on an approval step
`pantheon-service` SHALL allow company staff, or a construction site member whose active `SiteMembership` function matches the current cycle's lowest-order `PENDING` `OrcamentoApproval` step, to approve or reject that step. Approving the final step SHALL transition the Orçamento to `APPROVED` (Aprovado); approving a non-final step SHALL activate the next step. Rejecting SHALL require a reason, mark that step `REJECTED`, record the reason on the Orçamento, and transition the Orçamento back to `DRAFT` (Rascunho).

#### Scenario: Intermediate step approved
- **WHEN** the member matching a two-step chain's first step approves it
- **THEN** `pantheon-service` marks that step `APPROVED` and the Orçamento remains `IN_APPROVAL` with its second step now actionable

#### Scenario: Final step approved
- **WHEN** the member matching a chain's last remaining `PENDING` step approves it
- **THEN** `pantheon-service` marks that step `APPROVED` and transitions the Orçamento to `APPROVED`

#### Scenario: Step rejected returns Orçamento to Rascunho
- **WHEN** the member matching the current pending step rejects it, supplying a reason
- **THEN** `pantheon-service` marks that step `REJECTED`, records the reason on the Orçamento, and transitions the Orçamento to `DRAFT`

#### Scenario: Non-matching member blocked
- **WHEN** a construction site member whose function does not match the current pending step's function, and who is not company staff, attempts to approve or reject it
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Concluding an approved Orçamento
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to explicitly mark an `APPROVED` Orçamento as `COMPLETED` (Concluído). This action SHALL create one `Material` delivery-tracking record per `OrcamentoLineItem` on that Orçamento (see `material-delivery-tracking`).

#### Scenario: Approved Orçamento concluded
- **WHEN** a construction site member marks an `APPROVED` Orçamento as concluded
- **THEN** `pantheon-service` transitions it to `COMPLETED` and creates one `Material` record per line item

#### Scenario: Cannot conclude before approval
- **WHEN** a construction site member attempts to mark a `DRAFT` or `IN_APPROVAL` Orçamento as concluded
- **THEN** `pantheon-service` rejects the request

### Requirement: Orçamento listing and detail
`pantheon-service` SHALL allow any member of a construction site to list its Orçamentos and view one's full detail, including its line items and the approval-step history across every submission cycle.

#### Scenario: Member views an Orçamento's detail
- **WHEN** an authenticated member of a construction site requests a specific Orçamento by id
- **THEN** `pantheon-service` returns it with its line items and every approval-step record across all cycles

### Requirement: Orçamento workflow views
`pantheon-web` SHALL provide, on a construction site's "Orçamentos" tab, views to create an Orçamento and manage its line items while in Rascunho, submit it for approval, show the current approval step and act on it when the viewer is authorized, display the full approval history, and conclude an approved Orçamento, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member manages line items and submits from the UI
- **WHEN** a construction site member fills in one or more line items on a Rascunho Orçamento and clicks "Enviar para aprovação"
- **THEN** `pantheon-web` submits the Orçamento to `pantheon-service`, transitions it to "Em aprovação", and shows the first approval step as pending

#### Scenario: Authorized approver decides from the UI
- **WHEN** a user whose function matches the current pending approval step opens the Orçamento and approves or rejects it (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the Orçamento's displayed status and approval-step history

#### Scenario: Member concludes from the UI
- **WHEN** a construction site member opens an Aprovado Orçamento and clicks "Concluir"
- **THEN** `pantheon-web` submits the action to `pantheon-service`, shows the Orçamento as Concluído, and displays its generated delivery-tracking materials
