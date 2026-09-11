# material-delivery-tracking Specification

## Purpose
TBD - created by archiving change restructure-materials-orcamento-flow. Update Purpose after archive.
## Requirements
### Requirement: Material record creation on Orçamento conclusion
`pantheon-service` SHALL create one `Material` record for every `OrcamentoLineItem` of an Orçamento the moment it is marked `COMPLETED`, copying that line item's name, type, and quantity, and starting each in `AWAITING_DELIVERY` (Aguardando entrega) status. `Material` records are never created any other way.

#### Scenario: Materials created on conclusion
- **WHEN** an Orçamento with three line items is marked `COMPLETED`
- **THEN** `pantheon-service` creates three `Material` records, each `AWAITING_DELIVERY`, one per line item

### Requirement: Material delivery status progression
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to advance a `Material` record's status from `AWAITING_DELIVERY` to `DELIVERED` (Entregue), and from `DELIVERED` to `DELIVERED_AND_CHECKED` (Entregue e conferido), in that order.

#### Scenario: Marked as delivered
- **WHEN** a construction site member marks an `AWAITING_DELIVERY` Material as delivered
- **THEN** `pantheon-service` transitions it to `DELIVERED`

#### Scenario: Cannot skip straight to checked
- **WHEN** a construction site member attempts to mark an `AWAITING_DELIVERY` Material as `DELIVERED_AND_CHECKED` without it first being `DELIVERED`
- **THEN** `pantheon-service` rejects the request

### Requirement: Delivery-check photo attachment
`pantheon-service` SHALL allow a construction site member to attach one or more photos to a `Material` record when marking it `DELIVERED_AND_CHECKED`, storing each as a `MaterialDeliveryPhoto` in object storage.

#### Scenario: Photo attached on conference
- **WHEN** a construction site member marks a `DELIVERED` Material as checked and uploads a photo
- **THEN** `pantheon-service` transitions it to `DELIVERED_AND_CHECKED` and stores the photo linked to that Material

### Requirement: Material listing
`pantheon-service` SHALL allow any member of a construction site to list the `Material` records generated for that site, optionally filtered by their originating Orçamento, including each one's current delivery status.

#### Scenario: Member lists materials for a concluded Orçamento
- **WHEN** an authenticated member of a construction site requests the materials generated for one of its concluded Orçamentos
- **THEN** `pantheon-service` returns every `Material` record created from that Orçamento's line items, with its current status

### Requirement: Material delivery tracking views
`pantheon-web` SHALL provide, within a concluded Orçamento's detail view, a list of its generated materials with controls to advance each one's delivery status and attach conference photos, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member updates delivery status from the UI
- **WHEN** a construction site member opens a concluded Orçamento and marks one of its materials as delivered
- **THEN** `pantheon-web` submits the status change to `pantheon-service` and updates the material's displayed status

#### Scenario: Member checks a delivery with a photo from the UI
- **WHEN** a construction site member marks a delivered material as checked and attaches a photo
- **THEN** `pantheon-web` submits the status change and photo to `pantheon-service` and shows the material as "Entregue e conferido"

