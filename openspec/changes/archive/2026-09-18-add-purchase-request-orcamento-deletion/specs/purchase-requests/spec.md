## ADDED Requirements

### Requirement: Pedido de Compra deletion
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to permanently delete a `PurchaseRequest` header, together with all of its `PurchaseRequestItem`s, only while that header is `INICIADO`. `pantheon-service` SHALL reject a deletion attempt on a header in any other status (`ORCADO`, `CONFERIDO`, or `CONCLUIDO`).

#### Scenario: Deleting an Iniciado header
- **WHEN** a construction site member with `PURCHASE_REQUEST` access deletes a Pedido de Compra that is still `INICIADO`
- **THEN** `pantheon-service` permanently removes the `PurchaseRequest` and all of its `PurchaseRequestItem`s

#### Scenario: Cannot delete once Orçado or later
- **WHEN** a construction site member attempts to delete a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` rejects the request and leaves the header and its items unchanged

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to delete a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Deleting a Pedido de Compra from the UI
`pantheon-web` SHALL show an "Excluir" action on a Pedido de Compra's detail view only while it is `Iniciado`, prompt the member for confirmation before submitting the deletion, and, on success, navigate back to the Pedido de Compra list. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member deletes an Iniciado Pedido de Compra from the UI
- **WHEN** a construction site member opens an `Iniciado` Pedido de Compra, clicks "Excluir", and confirms the prompt
- **THEN** `pantheon-web` submits the deletion to `pantheon-service` and navigates back to the Pedido de Compra list, which no longer shows that header

#### Scenario: Delete action hidden once Orçado or later
- **WHEN** a construction site member opens a Pedido de Compra that is `Orçado`, `Conferido`, or `Concluído`
- **THEN** `pantheon-web` does not show the "Excluir" action
