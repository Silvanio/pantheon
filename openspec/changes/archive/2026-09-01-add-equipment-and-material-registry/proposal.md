## Why

The daily construction report and the material request workflow both need to reference concrete equipment and material types, but nothing today lets a project register what equipment it owns/rents or what materials it works with. This change adds those two catalogs, scoped to a `ConstructionSite`, as a prerequisite for both later changes.

## What Changes

- Add `Equipment`: belongs to a `ConstructionSite`. Fields: name, type/category, status (`AVAILABLE`, `IN_USE`, `MAINTENANCE`, `UNAVAILABLE`).
- Admins and Site Foreman members can register, list, and update the status of a site's `Equipment`.
- Add `Material` (material catalog entry): belongs to a `ConstructionSite`. Fields: name, unit of measure (e.g., m³, kg, un).
- Admins and Site Foreman members can register and list `Material` catalog entries within a construction site.
- `pantheon-web`: views for managing equipment and the material catalog within a construction site, copy sourced from the `pt-BR` locale file established in `add-construction-site-and-team`.

## Capabilities

### New Capabilities
- `equipment-material-registry`: Equipment and Material catalog entities scoped to a construction site, with registration/listing/status management.

### Modified Capabilities
(none)

## Impact

- **Affected code**: `pantheon-service` (new `Equipment`, `Material` entities/repositories/services/controllers/DTOs; new Flyway migrations), `pantheon-web` (new equipment and material catalog views under a construction site).
- **Depends on** `add-construction-site-and-team` (needs `ConstructionSite` to exist).
- **New REST surface**: `POST /api/construction-sites/{siteId}/equipment`, `GET /api/construction-sites/{siteId}/equipment`, `PATCH /api/equipment/{id}/status`, `POST /api/construction-sites/{siteId}/materials`, `GET /api/construction-sites/{siteId}/materials`.
- **Non-goals**: sharing a material/equipment catalog across multiple sites of the same project, equipment rental/ownership cost tracking, maintenance scheduling/history beyond the current status.
