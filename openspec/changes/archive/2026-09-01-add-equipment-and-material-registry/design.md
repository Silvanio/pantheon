## Context

This change is a small, self-contained catalog layer that `add-daily-construction-report` (equipment used, materials received) and `add-material-request-workflow` (material line items) both need to reference. It depends on `ConstructionSite` from `add-construction-site-and-team`. It follows that change's conventions: English entity/table/column/enum names, and any new `pantheon-web` copy sourced from the `pt-BR` locale resource file rather than hardcoded.

## Goals / Non-Goals

**Goals:**
- Give each construction site its own equipment and material catalogs.
- Keep both catalogs simple enough to be usable from day one without configuration overhead (no categories/taxonomies beyond a free-text type field).

**Non-Goals:**
- Cross-site shared catalogs.
- Equipment maintenance history, depreciation, or cost/rental tracking.
- Material stock/inventory levels (this change is a catalog of *types*, not a quantity ledger — quantity tracking happens where materials are actually used: the daily report and the material request workflow).

## Decisions

### Entity model
```
Equipment                                -- table: equipment
  id                    UUID PK
  construction_site_id  UUID FK construction_site.id
  name                  VARCHAR
  type                  VARCHAR NULL   -- free text, e.g. "Concrete mixer", "Scaffolding"
  status                VARCHAR        -- AVAILABLE | IN_USE | MAINTENANCE | UNAVAILABLE
  created_by            UUID FK app_user.id
  created_at / updated_at

Material                                 -- table: material
  id                    UUID PK
  construction_site_id  UUID FK construction_site.id
  name                  VARCHAR
  unit                  VARCHAR        -- e.g. "m3", "kg", "un", "bag"
  created_by            UUID FK app_user.id
  created_at / updated_at
```
Both are scoped to `ConstructionSite` (not `Project`) because equipment and material needs are typically site-specific and the product owner asked to keep this "simples e objetivo" — a shared cross-site catalog can be layered on later without a breaking change (it would just add an optional project-level default catalog). Table/column/enum names are English; `type`/`unit` values entered by users will typically be Portuguese words ("Betoneira", "sc" for saco) since they're free-text data, not code — that's expected and unrelated to the English-code convention, which applies to identifiers, not to end-user-entered content.

*Alternative considered*: scoping both to `Project` so multiple sites share one catalog — rejected for now since it adds a lookup/override layer with no current requirement driving it; site-scoped catalogs are simpler to reason about and match how the daily report is itself site-scoped.

### Permissions
Creation/status updates: `ADMIN` or a member whose `function` is `SITE_FOREMAN` (the role that operationally manages equipment/materials on site day-to-day). Listing: any project member.

*Alternative considered*: restrict to `ADMIN` only — rejected because in practice the site foreman is the person who actually manages equipment and materials day-to-day, not necessarily the project admin.

## Risks / Trade-offs

- **[Risk]** Free-text `type`/`unit` fields allow inconsistent data entry across sites → **Mitigation**: acceptable for this phase; a controlled vocabulary can be introduced later without breaking the schema (still plain strings).
- **[Risk]** Keying permission checks off `function = SITE_FOREMAN` introduces the first real permission use of the function field (previously descriptive-only, per the Open Question in `add-construction-site-and-team`) → **Mitigation**: this design treats it as an explicit, intentional decision for this change; documented here rather than silently assumed.

## Migration Plan

Additive only: new `equipment` and `material` tables via new Flyway migrations. No existing table/endpoint changes. Rollback: drop the two new tables.

## Open Questions

- Should a `MEMBER` with function `ENGINEER` also be allowed to manage equipment/materials, or is it strictly `ADMIN`/`SITE_FOREMAN`? This design assumes strictly `ADMIN`/`SITE_FOREMAN`; revisit if engineers need to log equipment status changes directly.
