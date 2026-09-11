## MODIFIED Requirements

### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT` and `DAILY_REPORT`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS` and `DAILY_REPORT` with no manage access on any material/orçamento capability; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT` only. (Authority to act on a specific Orçamento approval step is a separate, structural concern — see `orcamento-approval-workflow` — not one of these capabilities.)

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client has no material access by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports that the client cannot create purchase-request items

## ADDED Requirements

### Requirement: Permission enforcement on purchase requests and orçamento management
`pantheon-service` SHALL reject creating or converting a `PurchaseRequestItem` from a member with no `PURCHASE_REQUEST` access, and reject creating, editing, or submitting an `Orcamento` from a member with no `ORCAMENTO_MANAGE` access, regardless of their function.

#### Scenario: Member without purchase-request access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a purchase-request item
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without orçamento-management access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create or submit an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

## REMOVED Requirements

### Requirement: Permission enforcement on material requests and approval
**Reason**: `MATERIAL_REQUEST` and `MATERIAL_APPROVAL` are retired. Purchase-request creation is now gated by `PURCHASE_REQUEST` and Orçamento creation/management by `ORCAMENTO_MANAGE` (see the new "Permission enforcement on purchase requests and orçamento management" requirement in this capability); approval-step authority is no longer capability-gated at all — see `orcamento-approval-workflow`.
**Migration**: See this capability's "Permission enforcement on purchase requests and orçamento management" requirement, and `orcamento-approval-workflow`'s approval-step requirement.
