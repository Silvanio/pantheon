# orcamento-approval-workflow Specification

## Purpose
Defines the Orçamento (budget) lifecycle for a construction site: supplier-backed creation, line items, a configurable multi-step approval chain, and conclusion into delivery-tracking materials.

## Requirements
### Requirement: Orçamento creation
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to create an `Orcamento` for that site, either empty or pre-filled with line items copied from selected `PurchaseRequestItem`s of a single Pedido de Compra, starting in `DRAFT` (Rascunho) status. Creation SHALL require supplier data: a CNPJ and a name are mandatory, and an address and a contact name/phone are optional. `pantheon-service` SHALL resolve the supplier via find-or-create against the site's company's `Fornecedor` registry (see `supplier-registry`) and store a snapshot of the resolved supplier's fields on the Orçamento, together with a traceability reference to that `Fornecedor`.

#### Scenario: Orçamento created from scratch with a new supplier
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access creates a new Orçamento with no purchase-request items selected, supplying a CNPJ and name with no matching existing `Fornecedor`
- **THEN** `pantheon-service` persists a new `Orcamento` in `DRAFT` status with no line items, registers a new `Fornecedor`, and stores the supplier snapshot on the Orçamento

#### Scenario: Orçamento created reusing a known supplier
- **WHEN** a construction site member creates an Orçamento supplying a CNPJ that matches an existing `Fornecedor` of the site's company
- **THEN** `pantheon-service` reuses that `Fornecedor`, does not create a duplicate, and stores its fields as the Orçamento's supplier snapshot

#### Scenario: Cannot create without required supplier fields
- **WHEN** a construction site member attempts to create an Orçamento without a CNPJ or without a name
- **THEN** `pantheon-service` rejects the request

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
`pantheon-service` SHALL allow any member of a construction site to list its Orçamentos — optionally filtered to those created on a given calendar date and/or originating from a given Pedido de Compra — and view one's full detail, including its supplier snapshot, its originating Pedido de Compra (when any), its line items, and the approval-step history across every submission cycle.

#### Scenario: Member views an Orçamento's detail
- **WHEN** an authenticated member of a construction site requests a specific Orçamento by id
- **THEN** `pantheon-service` returns it with its supplier snapshot, originating Pedido de Compra reference (if any), line items, and every approval-step record across all cycles

#### Scenario: Member filters Orçamentos by date
- **WHEN** an authenticated member requests a site's Orçamentos filtered to a specific date
- **THEN** `pantheon-service` returns only the Orçamentos created on that site on that date

#### Scenario: Member filters Orçamentos by originating Pedido de Compra
- **WHEN** an authenticated member requests a site's Orçamentos filtered to a specific Pedido de Compra
- **THEN** `pantheon-service` returns only the Orçamentos whose `sourcePurchaseRequestId` matches that Pedido de Compra

### Requirement: Orçamento workflow views
`pantheon-web` SHALL provide, on a construction site's "Orçamentos" tab, views to: list Orçamentos filterable by date and by originating Pedido de Compra; create an Orçamento (blank or from selected Pedido de Compra items) by filling in supplier data with CNPJ-based autocomplete (see `supplier-registry`); manage its line items while in Rascunho; submit it for approval; show the current approval step and act on it when the viewer is authorized; display the full approval history; and conclude an approved Orçamento. The Orçamento detail view SHALL clearly present the supplier's data, a link to its originating Pedido de Compra when one exists, its status, its approval timeline, its line items with a computed total when unit prices are present, and its post-conclusion materials in a distinct section. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member manages line items and submits from the UI
- **WHEN** a construction site member fills in one or more line items on a Rascunho Orçamento and clicks "Enviar para aprovação"
- **THEN** `pantheon-web` submits the Orçamento to `pantheon-service`, transitions it to "Em aprovação", and shows the first approval step as pending

#### Scenario: Authorized approver decides from the UI
- **WHEN** a user whose function matches the current pending approval step opens the Orçamento and approves or rejects it (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the Orçamento's displayed status and approval-step history

#### Scenario: Member concludes from the UI
- **WHEN** a construction site member opens an Aprovado Orçamento and clicks "Concluir"
- **THEN** `pantheon-web` submits the action to `pantheon-service`, shows the Orçamento as Concluído, and displays its generated delivery-tracking materials in a distinct section

#### Scenario: Member filters the Orçamento list from the UI
- **WHEN** a construction site member sets a date filter or selects a Pedido de Compra filter on the Orçamentos list
- **THEN** `pantheon-web` shows only the matching Orçamentos

#### Scenario: Detail view links back to the originating Pedido de Compra
- **WHEN** a construction site member opens an Orçamento that was created from a Pedido de Compra
- **THEN** `pantheon-web` shows that Pedido de Compra's name as a link that navigates to it

