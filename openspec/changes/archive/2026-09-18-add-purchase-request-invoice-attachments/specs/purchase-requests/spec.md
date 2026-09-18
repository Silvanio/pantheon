## MODIFIED Requirements

### Requirement: Pedido de Compra deletion
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to permanently delete a `PurchaseRequest` header, together with all of its `PurchaseRequestItem`s and any `PurchaseRequestInvoice`s attached to it (including their stored files), only while that header is `INICIADO`. `pantheon-service` SHALL reject a deletion attempt on a header in any other status (`ORCADO`, `CONFERIDO`, or `CONCLUIDO`).

#### Scenario: Deleting an Iniciado header
- **WHEN** a construction site member with `PURCHASE_REQUEST` access deletes a Pedido de Compra that is still `INICIADO`
- **THEN** `pantheon-service` permanently removes the `PurchaseRequest` and all of its `PurchaseRequestItem`s

#### Scenario: Deleting a header also removes its attached invoices
- **WHEN** a construction site member deletes an `INICIADO` Pedido de Compra that has one or more `PurchaseRequestInvoice`s attached
- **THEN** `pantheon-service` deletes those invoices' stored files from object storage and their database rows, alongside the header and its items

#### Scenario: Cannot delete once Orçado or later
- **WHEN** a construction site member attempts to delete a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` rejects the request and leaves the header and its items unchanged

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to delete a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

## ADDED Requirements

### Requirement: Pedido de Compra invoice attachments
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` manage access to upload one or more invoice files (`PurchaseRequestInvoice`) to a `PurchaseRequest`, at any point in its lifecycle, storing each file in object storage together with its original filename and content type. Accepted file extensions SHALL be `pdf`, `xml`, `jpg`, `jpeg`, and `png`; any other extension, or an empty file, SHALL be rejected. `pantheon-service` SHALL allow any construction site member visible to `PURCHASE_REQUEST` to list a header's invoices and download one's content, and SHALL allow a member with `PURCHASE_REQUEST` manage access to delete one, removing both its database row and its stored file.

#### Scenario: Uploading an invoice
- **WHEN** a construction site member with `PURCHASE_REQUEST` access uploads a PDF file to a Pedido de Compra
- **THEN** `pantheon-service` stores the file in object storage and records a new `PurchaseRequestInvoice` with its original filename and content type, linked to that header

#### Scenario: Uploading regardless of header status
- **WHEN** a construction site member uploads an invoice to a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` accepts the upload the same way it would for an `INICIADO` header

#### Scenario: Rejecting a disallowed file type
- **WHEN** a construction site member attempts to upload a file whose extension is not `pdf`, `xml`, `jpg`, `jpeg`, or `png`
- **THEN** `pantheon-service` rejects the request

#### Scenario: Member lists and downloads invoices
- **WHEN** an authenticated member visible to `PURCHASE_REQUEST` requests a Pedido de Compra's invoices, then requests one's content
- **THEN** `pantheon-service` returns the list of attached invoices, and separately returns that invoice's stored bytes with its original filename and content type

#### Scenario: Deleting an invoice
- **WHEN** a construction site member with `PURCHASE_REQUEST` access deletes one of a header's invoices
- **THEN** `pantheon-service` removes that invoice's database row and deletes its file from object storage, leaving the header's other invoices (if any) untouched

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to upload or delete an invoice
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Pedido de Compra invoice section in the UI
`pantheon-web` SHALL provide, on the Pedido de Compra detail view, a "Notas fiscais" section — visible regardless of the header's status — listing every attached invoice with its filename and a download link, an upload control to attach a new file, and a remove action per invoice that confirms via the existing inline-popover pattern before deleting.

#### Scenario: Member uploads an invoice from the UI
- **WHEN** a construction site member selects a PDF file in the "Notas fiscais" section's upload control
- **THEN** `pantheon-web` uploads it to `pantheon-service` and shows it in the section's file list

#### Scenario: Member downloads an invoice from the UI
- **WHEN** a construction site member clicks an attached invoice's download link
- **THEN** `pantheon-web` fetches its content and saves it locally under its original filename

#### Scenario: Member removes an invoice from the UI
- **WHEN** a construction site member clicks an invoice's remove action and confirms the popover
- **THEN** `pantheon-web` deletes it and removes it from the section's file list
