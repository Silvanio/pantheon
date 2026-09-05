# material-request-workflow Specification

## Purpose

The material request/approval/receipt-conference workflow — a `MaterialRequest` raised against a construction site's `Material` catalog, approved or rejected by the project's administrator or an `ENGINEER`, and tracked to completion via per-line-item `ReceiptVerification` records that capture divergence between requested and received quantities. This workflow is deliberately separate from the daily construction report (`daily-construction-report`).
## Requirements
### Requirement: Material request creation
`pantheon-service` SHALL allow a member of a construction site with `MATERIAL_REQUEST` access to create a `MaterialRequest` with one or more line items, each referencing the site's `Material` catalog and a requested quantity, starting in `PENDING` status.

#### Scenario: Member creates a material request
- **WHEN** a construction site member with `MATERIAL_REQUEST` access submits one or more material line items (each a catalog reference and quantity) for that site
- **THEN** `pantheon-service` persists a new `MaterialRequest` in `PENDING` status with those line items

#### Scenario: Member without request access blocked
- **WHEN** a construction site member with no `MATERIAL_REQUEST` access attempts to create a material request
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Material request approval
`pantheon-service` SHALL allow a company administrator or a construction site member with `MATERIAL_APPROVAL` access to approve a pending material request.

#### Scenario: Admin approves a request
- **WHEN** a company administrator approves a pending material request on one of its construction sites
- **THEN** `pantheon-service` transitions the request's status to `APPROVED`

#### Scenario: Engineer approves a request
- **WHEN** a construction site member with function `ENGINEER` (which has `MATERIAL_APPROVAL` access by default) approves a pending material request
- **THEN** `pantheon-service` transitions the request's status to `APPROVED`

#### Scenario: Other members cannot approve
- **WHEN** a construction site member with no `MATERIAL_APPROVAL` access attempts to approve a pending material request
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Material request rejection
`pantheon-service` SHALL allow a company administrator or a construction site member with `MATERIAL_APPROVAL` access to reject a pending material request with a required reason.

#### Scenario: Request rejected with reason
- **WHEN** a company administrator or a member with `MATERIAL_APPROVAL` access rejects a pending material request, supplying a reason
- **THEN** `pantheon-service` transitions the request's status to `REJECTED` and records the reason

#### Scenario: Rejection without a reason is refused
- **WHEN** a company administrator or a member with `MATERIAL_APPROVAL` access attempts to reject a pending material request without supplying a reason
- **THEN** `pantheon-service` rejects the rejection request itself and does not change the material request's status

### Requirement: Received-materials verification
`pantheon-service` SHALL allow a construction site member to record a `ReceiptVerification` against a line item of an approved material request, capturing the quantity actually received and, optionally, one or more delivery-proof photos.

#### Scenario: Line item verified
- **WHEN** a construction site member submits a received quantity for a line item of an approved material request
- **THEN** `pantheon-service` records the verification for that line item, including any divergence from the requested quantity

#### Scenario: Delivery-proof photo attached
- **WHEN** a construction site member uploads a photo alongside a receipt verification
- **THEN** `pantheon-service` stores the photo in object storage and links it to that verification as a `ReceiptVerificationPhoto`

#### Scenario: Cannot verify a non-approved request
- **WHEN** a construction site member attempts to record a verification against a line item of a request that is not `APPROVED`, `PARTIALLY_RECEIVED`, or `RECEIVED`
- **THEN** `pantheon-service` rejects the request

#### Scenario: Cannot verify the same line item twice
- **WHEN** a construction site member attempts to record a second verification against a line item that already has one
- **THEN** `pantheon-service` rejects the request with HTTP 409

### Requirement: Material request status tracking
`pantheon-service` SHALL update a material request's status to `PARTIALLY_RECEIVED` once at least one, but not all, of its line items have a verification record, and to `RECEIVED` once every line item does.

#### Scenario: Partial receipt
- **WHEN** a verification is recorded for one line item of a multi-item approved request while others remain unverified
- **THEN** `pantheon-service` sets the request's status to `PARTIALLY_RECEIVED`

#### Scenario: Full receipt
- **WHEN** a verification is recorded for the last remaining unverified line item of an approved request
- **THEN** `pantheon-service` sets the request's status to `RECEIVED`

### Requirement: Material request listing and detail
`pantheon-service` SHALL allow any member of a project to list a construction site's material requests (optionally filtered by status) and view one request's full detail, including its line items and any verification records.

#### Scenario: Member lists material requests
- **WHEN** an authenticated member of a project requests the list of material requests for one of its construction sites
- **THEN** `pantheon-service` returns every `MaterialRequest` for that site, including its current status

#### Scenario: Member views a request's detail
- **WHEN** an authenticated member of a project requests a specific material request by id
- **THEN** `pantheon-service` returns the request with its line items and any recorded verifications

### Requirement: Material request workflow views
`pantheon-web` SHALL provide views to create a material request, list/track requests by status, approve or reject a pending request, and record a receipt verification against an approved request's line items, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a request from the UI
- **WHEN** a project member fills in one or more material line items and submits the request creation form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new request in the site's request list with status `PENDING`

#### Scenario: Admin approves or rejects from the UI
- **WHEN** an authorized user opens a pending request and chooses approve or reject (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the request's displayed status

#### Scenario: Member records a receipt verification from the UI
- **WHEN** a project member opens an approved request and submits received quantities for its line items
- **THEN** `pantheon-web` submits the verification to `pantheon-service` and updates the request's displayed status and any divergence flags

### Requirement: Budget (orçamento) creation against a request
`pantheon-service` SHALL allow a construction site member with `MATERIAL_REQUEST` access to draft an `Orcamento` against a `MaterialRequest`, with per-line-item unit pricing, starting in `DRAFT` status. A request MAY have more than one `Orcamento` over time (e.g., a revised quote after a rejection).

#### Scenario: Budget drafted against a request
- **WHEN** a construction site member submits unit prices for a material request's line items
- **THEN** `pantheon-service` persists a new `Orcamento` in `DRAFT` status linked to that request, with priced line items

### Requirement: Sending a budget to the client
`pantheon-service` SHALL allow a construction site member with `MATERIAL_REQUEST` access to send a `DRAFT` `Orcamento` to the request's client, transitioning it to `SENT` and notifying the client.

#### Scenario: Budget sent
- **WHEN** a construction site member sends a draft budget
- **THEN** `pantheon-service` transitions the budget's status to `SENT` and publishes an event so the client is notified

### Requirement: Client approval of a budget
`pantheon-service` SHALL allow the client `SiteMembership` associated with a construction site to approve or reject a `SENT` `Orcamento`, requiring a reason on rejection. Approving a budget SHALL transition its `MaterialRequest` to `APPROVED`.

#### Scenario: Client approves a budget
- **WHEN** the site's client approves a `SENT` budget
- **THEN** `pantheon-service` transitions the budget to `APPROVED` and its material request to `APPROVED`

#### Scenario: Client rejects a budget with a reason
- **WHEN** the site's client rejects a `SENT` budget, supplying a reason
- **THEN** `pantheon-service` transitions the budget to `REJECTED` and records the reason, leaving the material request `PENDING` for a possible revised budget

#### Scenario: Non-client cannot decide
- **WHEN** a construction site member who is not the request's client attempts to approve or reject a budget
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Payment proof and invoice attachments
`pantheon-service` SHALL allow a construction site member to attach one or more payment-proof or invoice ("nota fiscal") files to an `Orcamento`, stored in object storage and tagged with their kind.

#### Scenario: Payment proof attached
- **WHEN** a construction site member uploads a payment-proof file to an approved budget
- **THEN** `pantheon-service` stores the file in object storage and records an `OrcamentoAttachment` of kind `PAYMENT_PROOF`

#### Scenario: Invoice attached
- **WHEN** a construction site member uploads an invoice file to an approved budget
- **THEN** `pantheon-service` stores the file in object storage and records an `OrcamentoAttachment` of kind `INVOICE`

### Requirement: Budget and orçamento workflow views
`pantheon-web` SHALL provide, within a construction site's materials menu, views to draft a budget against a request, send it to the client, for the client to approve or reject it, and to attach payment-proof/invoice files and delivery-proof photos, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member drafts and sends a budget from the UI
- **WHEN** a construction site member fills in unit prices for a request's line items and clicks "Enviar ao cliente"
- **THEN** `pantheon-web` submits the budget to `pantheon-service`, transitions it to `SENT`, and shows it as awaiting the client's decision

#### Scenario: Client decides from the UI
- **WHEN** the client opens a sent budget and approves or rejects it (supplying a reason for rejection)
- **THEN** `pantheon-web` submits the decision to `pantheon-service` and updates the budget's and request's displayed status

