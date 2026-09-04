# material-request-workflow Specification

## Purpose

The material request/approval/receipt-conference workflow — a `MaterialRequest` raised against a construction site's `Material` catalog, approved or rejected by the project's administrator or an `ENGINEER`, and tracked to completion via per-line-item `ReceiptVerification` records that capture divergence between requested and received quantities. This workflow is deliberately separate from the daily construction report (`daily-construction-report`).

## Requirements

### Requirement: Material request creation
`pantheon-service` SHALL allow a member of a construction site's project to create a `MaterialRequest` with one or more line items, each referencing the site's `Material` catalog and a requested quantity, starting in `PENDING` status.

#### Scenario: Member creates a material request
- **WHEN** a project member submits one or more material line items (each a catalog reference and quantity) for a construction site
- **THEN** `pantheon-service` persists a new `MaterialRequest` in `PENDING` status with those line items

### Requirement: Material request approval
`pantheon-service` SHALL allow the administrator of the project or a member with construction function `ENGINEER` to approve a pending material request.

#### Scenario: Admin approves a request
- **WHEN** the administrator of a project approves a pending material request
- **THEN** `pantheon-service` transitions the request's status to `APPROVED`

#### Scenario: Engineer approves a request
- **WHEN** a project member with construction function `ENGINEER` approves a pending material request
- **THEN** `pantheon-service` transitions the request's status to `APPROVED`

#### Scenario: Other members cannot approve
- **WHEN** a project member who is neither the administrator nor an `ENGINEER` attempts to approve a pending material request
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Material request rejection
`pantheon-service` SHALL allow the administrator of the project or a member with construction function `ENGINEER` to reject a pending material request with a required reason.

#### Scenario: Request rejected with reason
- **WHEN** the administrator or an `ENGINEER` member rejects a pending material request, supplying a reason
- **THEN** `pantheon-service` transitions the request's status to `REJECTED` and records the reason

#### Scenario: Rejection without a reason is refused
- **WHEN** the administrator or an `ENGINEER` member attempts to reject a pending material request without supplying a reason
- **THEN** `pantheon-service` rejects the rejection request itself and does not change the material request's status

### Requirement: Received-materials verification
`pantheon-service` SHALL allow a project member to record a `ReceiptVerification` against a line item of an approved material request, capturing the quantity actually received.

#### Scenario: Line item verified
- **WHEN** a project member submits a received quantity for a line item of an approved material request
- **THEN** `pantheon-service` records the verification for that line item, including any divergence from the requested quantity

#### Scenario: Cannot verify a non-approved request
- **WHEN** a project member attempts to record a verification against a line item of a request that is not `APPROVED`, `PARTIALLY_RECEIVED`, or `RECEIVED`
- **THEN** `pantheon-service` rejects the request

#### Scenario: Cannot verify the same line item twice
- **WHEN** a project member attempts to record a second verification against a line item that already has one
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
