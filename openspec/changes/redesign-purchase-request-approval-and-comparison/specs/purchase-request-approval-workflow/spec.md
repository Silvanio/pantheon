## ADDED Requirements

### Requirement: Per-site Pedido de Compra approval levels
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to configure an ordered list of Pedido de Compra approval levels for that site, each specifying a step order and a required construction-site function. A site with no configured levels SHALL default to a single implicit level requiring the `ENGINEER` function.

#### Scenario: Admin configures a two-step chain
- **WHEN** a company staff member configures a site's approval levels as step 1 = `ENGINEER`, step 2 = `CLIENT`
- **THEN** `pantheon-service` persists both levels in that order for that site

#### Scenario: Unconfigured site defaults to a single level
- **WHEN** a Pedido de Compra is submitted for approval on a site with no configured approval levels
- **THEN** `pantheon-service` creates a single approval step requiring the `ENGINEER` function

### Requirement: Submitting a Pedido de Compra for approval
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to submit an `ORCADO` Pedido de Compra for approval, transitioning it to a pending-approval state and creating one `PurchaseRequestApproval` record per configured approval level (or the default level), in order, all starting `PENDING`, tagged with a new approval cycle number. Submission SHALL require that every `PurchaseRequestItem` of the header has a non-null `selectedOrcamentoLineItemId`.

#### Scenario: Fully selected Pedido de Compra submitted
- **WHEN** a construction site member submits an `ORCADO` Pedido de Compra whose every item has a selected Orçamento line item
- **THEN** `pantheon-service` creates its ordered approval steps for a new cycle and marks the header pending approval

#### Scenario: Cannot submit before every item is selected
- **WHEN** a construction site member attempts to submit an `ORCADO` Pedido de Compra that has at least one item with no selected Orçamento line item
- **THEN** `pantheon-service` rejects the request

#### Scenario: Cannot submit a Pedido de Compra with no Orçamentos yet
- **WHEN** a construction site member attempts to submit an `INICIADO` Pedido de Compra (no Orçamento has been created for it yet)
- **THEN** `pantheon-service` rejects the request

### Requirement: Acting on an approval step
`pantheon-service` SHALL allow company staff, or a construction site member whose active `SiteMembership` function matches the current cycle's lowest-order `PENDING` `PurchaseRequestApproval` step, to approve or reject that step. Approving the final step SHALL transition the Pedido de Compra to `CONFERIDO` and lock every Orçamento linked to it; approving a non-final step SHALL activate the next step. Rejecting SHALL require a reason, mark that step `REJECTED`, record the reason on the Pedido de Compra, transition it back to `ORCADO`, and unlock every Orçamento linked to it.

#### Scenario: Intermediate step approved
- **WHEN** the member matching a two-step chain's first step approves it
- **THEN** `pantheon-service` marks that step `APPROVED` and the Pedido de Compra remains pending approval with its second step now actionable

#### Scenario: Final step approved
- **WHEN** the member matching a chain's last remaining `PENDING` step approves it
- **THEN** `pantheon-service` marks that step `APPROVED`, transitions the Pedido de Compra to `CONFERIDO`, and locks its linked Orçamentos

#### Scenario: Step rejected returns Pedido de Compra to Orçado
- **WHEN** the member matching the current pending step rejects it, supplying a reason
- **THEN** `pantheon-service` marks that step `REJECTED`, records the reason on the Pedido de Compra, transitions it to `ORCADO`, and unlocks its linked Orçamentos

#### Scenario: Non-matching member blocked
- **WHEN** a construction site member whose function does not match the current pending step's function, and who is not company staff, attempts to approve or reject it
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Concluding an approved Pedido de Compra
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to explicitly mark a `CONFERIDO` Pedido de Compra as `CONCLUIDO`. This action SHALL create one `Material` delivery-tracking record per `PurchaseRequestItem` of that header, using the name, type, quantity, and unit price of each item's selected `OrcamentoLineItem`.

#### Scenario: Approved Pedido de Compra concluded
- **WHEN** a construction site member marks a `CONFERIDO` Pedido de Compra as concluded
- **THEN** `pantheon-service` transitions it to `CONCLUIDO` and creates one `Material` record per item, sourced from each item's selected Orçamento line item

#### Scenario: Cannot conclude before approval
- **WHEN** a construction site member attempts to mark an `INICIADO`, `ORCADO`, or pending-approval Pedido de Compra as concluded
- **THEN** `pantheon-service` rejects the request

### Requirement: Pedido de Compra approval views
`pantheon-web` SHALL show, on a Pedido de Compra's detail view, a status stepper (Iniciado → Orçado → Conferido → Concluído), a summary of the currently selected item-to-supplier mix once submission is possible, the current approval step and history when the header has been submitted at least once, actions to approve or reject a step when the viewer is authorized (rejection requiring a reason), and an action to conclude a `CONFERIDO` Pedido de Compra. Once `CONCLUIDO`, the view SHALL show the generated delivery-tracking materials in a distinct section. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member submits the selected mix from the UI
- **WHEN** a construction site member on an `ORCADO` Pedido de Compra with every item selected clicks "Enviar para aprovação"
- **THEN** `pantheon-web` submits it to `pantheon-service`, and shows the first approval step as pending

#### Scenario: Authorized approver decides from the UI
- **WHEN** a user whose function matches the current pending approval step opens the Pedido de Compra and approves or rejects it (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the displayed status and approval-step history

#### Scenario: Member concludes from the UI
- **WHEN** a construction site member opens a `CONFERIDO` Pedido de Compra and clicks "Concluir"
- **THEN** `pantheon-web` submits the action to `pantheon-service`, shows the Pedido de Compra as `CONCLUIDO`, and displays its generated delivery-tracking materials in a distinct section
