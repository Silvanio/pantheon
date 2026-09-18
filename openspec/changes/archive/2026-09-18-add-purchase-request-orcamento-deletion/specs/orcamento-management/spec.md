## ADDED Requirements

### Requirement: Orçamento deletion
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to permanently delete an `Orcamento`, together with all of its `OrcamentoLineItem`s, only while it is `DRAFT`. `pantheon-service` SHALL reject a deletion attempt on a `LOCKED` Orçamento.

When the deleted Orçamento has a `sourcePurchaseRequestId`, `pantheon-service` SHALL additionally, in the same transaction:
- for every `PurchaseRequestItem` of that header whose `convertedToOrcamentoId` equals the deleted Orçamento's id, revert it to `PENDING` status and clear its `convertedToOrcamentoId` and `convertedAt`;
- clear `selectedOrcamentoLineItemId` on any `PurchaseRequestItem` of that header that referenced one of the deleted Orçamento's line items;
- if, after the deletion, no `Orcamento` remains linked to that header, transition the header from `ORCADO` back to `INICIADO`.

#### Scenario: Deleting a standalone Draft Orçamento
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access deletes a `DRAFT` Orçamento with no `sourcePurchaseRequestId`
- **THEN** `pantheon-service` permanently removes the Orçamento and all of its line items

#### Scenario: Deleting a converted Orçamento reverts its items to Pendente
- **WHEN** a construction site member deletes a `DRAFT` Orçamento that was created from two `PurchaseRequestItem`s of a Pedido de Compra
- **THEN** `pantheon-service` removes the Orçamento and reverts both items to `PENDING` status with no `convertedToOrcamentoId`, making them available for a later conversion

#### Scenario: Deleting a converted Orçamento clears stale selections
- **WHEN** a construction site member deletes a `DRAFT` Orçamento whose line item is currently the `selectedOrcamentoLineItemId` of a Pedido de Compra item
- **THEN** `pantheon-service` clears that item's `selectedOrcamentoLineItemId`

#### Scenario: Deleting the last linked Orçamento reverts the header to Iniciado
- **WHEN** a construction site member deletes the only Orçamento linked to an `ORCADO` Pedido de Compra
- **THEN** `pantheon-service` transitions that header back to `INICIADO`

#### Scenario: Deleting one of several linked Orçamentos leaves the header Orçado
- **WHEN** a construction site member deletes one of two Orçamentos linked to an `ORCADO` Pedido de Compra
- **THEN** `pantheon-service` leaves the header's status as `ORCADO`, since the other Orçamento is still linked

#### Scenario: Cannot delete a Locked Orçamento
- **WHEN** a construction site member attempts to delete a `LOCKED` Orçamento
- **THEN** `pantheon-service` rejects the request and leaves the Orçamento and its line items unchanged

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to delete an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Deleting an Orçamento from the UI
`pantheon-web` SHALL show an "Excluir" action on an Orçamento's detail view only while it is `DRAFT` ("Rascunho"), prompt the member for confirmation before submitting the deletion, and, on success, navigate back to the Orçamento list. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member deletes a Draft Orçamento from the UI
- **WHEN** a construction site member opens a `DRAFT` Orçamento, clicks "Excluir", and confirms the prompt
- **THEN** `pantheon-web` submits the deletion to `pantheon-service` and navigates back to the Orçamento list, which no longer shows that Orçamento

#### Scenario: Delete action hidden once Locked
- **WHEN** a construction site member opens a `LOCKED` Orçamento
- **THEN** `pantheon-web` does not show the "Excluir" action
