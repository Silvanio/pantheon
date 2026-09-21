# purchase-request-approval-workflow Specification

## Purpose
Defines the approval workflow for a Pedido de Compra: configurable per-site multi-step approval levels, submission gated on every item having a selected Orçamento line item, acting on approval steps (which locks or unlocks the Pedido de Compra's linked Orçamentos), and concluding an approved Pedido de Compra into delivery-tracking materials.
## Requirements
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
`pantheon-service` SHALL allow a user to approve or reject the current cycle's lowest-order `PENDING` `PurchaseRequestApproval` step only when both hold: (1) either the user holds no active `SiteMembership` on that construction site (company staff acting with no site role of their own), or the user holds an active `SiteMembership` on that site whose function matches the step's function — this function match is required with no exception, even for company staff who also hold a `SiteMembership` there; and (2) the user's resolved `PURCHASE_REQUEST` access level is `MANAGE` or `VIEW_AND_APPROVE` (never `VIEW` or `HIDDEN`). Approving the final step SHALL transition the Pedido de Compra to `CONFERIDO` and lock every Orçamento linked to it; approving a non-final step SHALL activate the next step. Rejecting SHALL require a reason, mark that step `REJECTED`, record the reason on the Pedido de Compra, transition it back to `ORCADO`, and unlock every Orçamento linked to it. When the acting user has an active `SiteMembership` on the site, the decision SHALL be attributed to that `SiteMembership`, even if the user is also company staff.

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

#### Scenario: Company staff with a matching site role decides as that role
- **WHEN** a user who has an active `SiteMembership` with function `CLIENT` on a site, and who is also company staff for that site's company, approves the current pending step and that step requires the `CLIENT` function
- **THEN** `pantheon-service` allows the approval and attributes it to that user's `CLIENT` `SiteMembership`

#### Scenario: Company staff with a non-matching site role is blocked, even if company staff
- **WHEN** a user who has an active `SiteMembership` with function `CLIENT` on a site, and who is also company staff for that site's company, attempts to approve a pending step that requires the `ENGINEER` function
- **THEN** `pantheon-service` rejects the request with HTTP 403, regardless of the user's company-staff status

#### Scenario: Company staff with no site role at all retains the approval bypass
- **WHEN** a user who is company staff for a site's company, and has no active `SiteMembership` on that specific site, approves the current pending step for any function
- **THEN** `pantheon-service` allows the approval and attributes it to no specific `SiteMembership`

#### Scenario: View-only access blocks approval even on a function match
- **WHEN** a construction site member whose function matches the current pending step, but whose resolved `PURCHASE_REQUEST` access is `VIEW`, attempts to approve or reject it
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Concluding an approved Pedido de Compra
`pantheon-service` SHALL allow a construction site member whose resolved `PURCHASE_REQUEST` access is `MANAGE` or `VIEW_AND_APPROVE` — the same access levels allowed to act on an approval step — to explicitly mark a `CONFERIDO` Pedido de Compra as `CONCLUIDO`. A member whose resolved access is `VIEW` or `HIDDEN` SHALL be rejected, even though `VIEW` can read the Pedido de Compra. This action SHALL create one `Material` delivery-tracking record per `PurchaseRequestItem` of that header, using the name, type, quantity, and unit price of each item's selected `OrcamentoLineItem`.

#### Scenario: Approved Pedido de Compra concluded
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `MANAGE` marks a `CONFERIDO` Pedido de Compra as concluded
- **THEN** `pantheon-service` transitions it to `CONCLUIDO` and creates one `Material` record per item, sourced from each item's selected Orçamento line item

#### Scenario: A view-and-approve member can also conclude
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW_AND_APPROVE` marks a `CONFERIDO` Pedido de Compra (already visible to them, since it is `CONFERIDO`/on its way to `CONCLUIDO`) as concluded
- **THEN** `pantheon-service` transitions it to `CONCLUIDO`, the same as for a `MANAGE` member

#### Scenario: A view-only member cannot conclude
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW` attempts to mark a `CONFERIDO` Pedido de Compra as concluded
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Cannot conclude before approval
- **WHEN** a construction site member attempts to mark an `INICIADO`, `ORCADO`, or pending-approval Pedido de Compra as concluded
- **THEN** `pantheon-service` rejects the request

### Requirement: Pedido de Compra approval views
`pantheon-web` SHALL show, on a Pedido de Compra's detail view, a status stepper (Iniciado → Orçado → Conferido → Concluído), a summary of the currently selected item-to-supplier mix once submission is possible, the current approval step and history when the header has been submitted at least once, actions to approve or reject a step when the viewer is authorized (rejection requiring a reason), and an action to conclude a `CONFERIDO` Pedido de Compra when the viewer's resolved `PURCHASE_REQUEST` access is `MANAGE` or `VIEW_AND_APPROVE` (never `VIEW` or `HIDDEN`) — the same access levels allowed to act on an approval step, per `pantheon-service`'s "Concluding an approved Pedido de Compra" check. "Authorized" to approve or reject means: the viewer's resolved `PURCHASE_REQUEST` access is `MANAGE` or `VIEW_AND_APPROVE`, and either their own `SiteMembership` function on that site matches the current pending step's function, or (only when they hold no `SiteMembership` on that site at all) they are company staff. A viewer who is not authorized SHALL NOT see the approve/reject actions at all, even if the Pedido de Compra itself is visible to them — this mirrors `pantheon-service`'s "Acting on an approval step" check so the UI never offers an action the backend would reject. Once `CONCLUIDO`, the view SHALL show the generated delivery-tracking materials in a distinct section. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member submits the selected mix from the UI
- **WHEN** a construction site member on an `ORCADO` Pedido de Compra with every item selected clicks "Enviar para aprovação"
- **THEN** `pantheon-web` submits it to `pantheon-service`, and shows the first approval step as pending

#### Scenario: Authorized approver decides from the UI
- **WHEN** a user whose function matches the current pending approval step, and whose resolved `PURCHASE_REQUEST` access allows it, opens the Pedido de Compra and approves or rejects it (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the displayed status and approval-step history

#### Scenario: A different step's approver does not see approve/reject actions
- **WHEN** a construction site member whose function matches an *earlier or later* step, but not the current pending step, opens a Pedido de Compra that is visible to them (e.g. because their own step already decided, or their access is `MANAGE`)
- **THEN** `pantheon-web` shows the current pending step's status but does not show approve/reject actions to that member

#### Scenario: Member concludes from the UI
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `MANAGE` or `VIEW_AND_APPROVE` opens a `CONFERIDO` Pedido de Compra and clicks "Concluir"
- **THEN** `pantheon-web` submits the action to `pantheon-service`, shows the Pedido de Compra as `CONCLUIDO`, and displays its generated delivery-tracking materials in a distinct section

#### Scenario: View-only member does not see the conclude action
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW` opens a `CONFERIDO` Pedido de Compra
- **THEN** `pantheon-web` shows the status stepper but does not show the "Concluir" button to that member

### Requirement: Dynamic visibility for view-and-approve members
`pantheon-service` SHALL only let a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW_AND_APPROVE` read a given Pedido de Compra (via listing, direct lookup, the comparison view, or its invoices) when the Pedido de Compra is `CONCLUIDO`, when that member's function already decided (`APPROVED` or `REJECTED`) a step of the Pedido de Compra's current approval cycle, or when that member's function matches the current cycle's currently actionable step — the lowest-order step still `PENDING`. Every configured level's `PurchaseRequestApproval` is created `PENDING` at submission time, all at once, so a step existing for a member's function is not by itself sufficient: only the lowest-order `PENDING` step is actionable at any moment, and a member's function having a `PENDING` row that is *not* that lowest-order step does not grant visibility. A Pedido de Compra that meets none of these conditions SHALL behave, for that member, as if it does not exist. This check is scoped to the Pedido de Compra's `currentApprovalCycle`: a step from an earlier, superseded cycle does not by itself grant visibility.

#### Scenario: Not yet the view-and-approve member's turn
- **WHEN** a `VIEW_AND_APPROVE` member whose function is `CLIENT` requests a Pedido de Compra whose current cycle has both an `ENGINEER` step and a `CLIENT` step already created `PENDING`, but the `ENGINEER` step (lower step order) is the one still actionable
- **THEN** `pantheon-service` responds as if that Pedido de Compra does not exist, even though a `PENDING` `CLIENT` step already exists in that cycle

#### Scenario: Now the view-and-approve member's turn
- **WHEN** the `ENGINEER` step above is approved, making the already-existing `PENDING` `CLIENT` step the cycle's new lowest-order `PENDING` step
- **THEN** the same `VIEW_AND_APPROVE` `CLIENT` member can now read that Pedido de Compra

#### Scenario: Visibility persists after the member has decided
- **WHEN** a `VIEW_AND_APPROVE` member has already approved or rejected the step matching their function in the current cycle
- **THEN** they can continue to read that Pedido de Compra for the remainder of that cycle, even after later steps move past their function

#### Scenario: Concluded purchase requests are always visible
- **WHEN** a `VIEW_AND_APPROVE` member requests a `CONCLUIDO` Pedido de Compra, regardless of whether any approval step ever matched their function
- **THEN** `pantheon-service` returns it

#### Scenario: List excludes irrelevant purchase requests
- **WHEN** a `VIEW_AND_APPROVE` member lists a site's Pedidos de Compra
- **THEN** `pantheon-service` excludes every Pedido de Compra that is neither `CONCLUIDO`, nor already decided by that member's function in the current cycle, nor currently actionable by that member's function

### Requirement: Approval-step transitions trigger a push notification
In addition to their existing effects, submitting a Pedido de Compra for approval, approving a step, rejecting a step, and concluding a Pedido de Compra SHALL each trigger a best-effort push notification (via the `push-notifications` capability) to the affected user(s): the site members whose function matches the newly-pending step's approver function on submit/approve, and the request's creator on reject/conclude. A failure or absence of push configuration SHALL NOT affect the underlying state transition.

#### Scenario: Submitting for approval notifies the first approver
- **WHEN** a Pedido de Compra is submitted for approval and its first step's approver function is ENGINEER
- **THEN** the site's members with an active ENGINEER `SiteMembership` and a registered device token receive a push notification, and the submission itself succeeds regardless of push delivery outcome

#### Scenario: Rejecting a step notifies the creator
- **WHEN** an approver rejects the pending step of a Pedido de Compra
- **THEN** the request's creator receives a push notification if they have a registered device token, and the rejection itself succeeds regardless of push delivery outcome

