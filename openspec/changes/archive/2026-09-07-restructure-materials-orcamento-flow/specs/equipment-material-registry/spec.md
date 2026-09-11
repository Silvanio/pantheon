## MODIFIED Requirements

### Requirement: Equipment registration
`pantheon-service` SHALL allow a company administrator or a construction site member with `MANAGE` access to that site's `EQUIPMENT` capability (by default, `SITE_FOREMAN`) to register `Equipment` under that construction site, recording a name, an optional type, and a status.

#### Scenario: Admin registers equipment
- **WHEN** a company administrator submits a name and status for a new piece of equipment on one of its construction sites
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Site Foreman registers equipment
- **WHEN** a construction site member with function `SITE_FOREMAN` submits a name and status for a new piece of equipment on a site they belong to
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Other members cannot register equipment
- **WHEN** a construction site member with `VIEW`-only `EQUIPMENT` access attempts to register equipment
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Equipment status update
`pantheon-service` SHALL allow a company administrator or a member with `MANAGE` access to a site's `EQUIPMENT` capability to update an `Equipment` record's status among `AVAILABLE`, `IN_USE`, `MAINTENANCE`, and `UNAVAILABLE`.

#### Scenario: Status updated
- **WHEN** a company administrator or a member with `EQUIPMENT` management access submits a new status for an existing piece of equipment
- **THEN** `pantheon-service` updates the equipment's status to the submitted value

## ADDED Requirements

### Requirement: Equipment views
`pantheon-web` SHALL provide, on its own construction-site tab independent of any material or budget feature, views for registering and listing equipment and for updating an equipment's status, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: User manages equipment from the UI
- **WHEN** a user with permission to manage equipment submits the equipment registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new equipment in the site's equipment list, on the "Equipamentos" tab

## REMOVED Requirements

### Requirement: Material catalog registration
**Reason**: The material catalog is removed. Materials are no longer cadastro'd — they exist only as free-text line items entered directly on a Pedido de Compra item or an Orçamento line item.
**Migration**: See `purchase-requests`' purchase-request-item creation and `orcamento-approval-workflow`'s line-item requirements.

### Requirement: List material catalog of a construction site
**Reason**: The material catalog is removed; there is nothing left to list.
**Migration**: See `purchase-requests` and `orcamento-approval-workflow` for the free-text lists that replace it.

### Requirement: Equipment and material catalog views
**Reason**: Split in two: equipment now has its own standalone tab/view (see the new "Equipment views" requirement in this capability); the material-catalog view is removed along with the catalog itself.
**Migration**: See this capability's "Equipment views" requirement, and `purchase-requests`'/`orcamento-approval-workflow`'s UI requirements for the free-text material entry that replaces the catalog form.
