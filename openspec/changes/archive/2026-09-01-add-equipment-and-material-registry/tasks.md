## 1. Backend: Equipment

- [x] 1.1 Create `equipment` table via Flyway migration (construction_site_id, name, type, status, created_by, timestamps)
- [x] 1.2 Add `Equipment` entity, repository, service, DTOs
- [x] 1.3 Add controller endpoints: `POST /api/construction-sites/{siteId}/equipment`, `GET /api/construction-sites/{siteId}/equipment`, `PATCH /api/equipment/{id}/status`
- [x] 1.4 Enforce ADMIN/SITE_FOREMAN-only create/status-update, member-readable listing
- [x] 1.5 Unit tests for creation, status update, and permission checks — 7/7 green

## 2. Backend: Material catalog

- [x] 2.1 Create `material` table via Flyway migration (construction_site_id, name, unit, created_by, timestamps)
- [x] 2.2 Add `Material` entity, repository, service, DTOs
- [x] 2.3 Add controller endpoints: `POST /api/construction-sites/{siteId}/materials`, `GET /api/construction-sites/{siteId}/materials`
- [x] 2.4 Enforce ADMIN/SITE_FOREMAN-only creation, member-readable listing
- [x] 2.5 Unit tests for creation and permission checks — 5/5 green

## 3. Frontend

- [x] 3.1 Add equipment list/create view under a construction site, with status update control, copy sourced from `pt-BR.json` — `EquipmentPanel.vue`, expandable per site row in `ConstructionSitesPanel.vue`
- [x] 3.2 Add material catalog list/create view under a construction site, copy sourced from `pt-BR.json` — `MaterialsPanel.vue`, same expandable section
- [x] 3.3 Add API client functions for the new endpoints

## 4. Verification

- [x] 4.1 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes — full suite green (EquipmentServiceTest 7/7, MaterialServiceTest 5/5, plus all prior suites)
- [x] 4.2 Build `pantheon-web` (`npm run build`) and confirm it passes — clean, 0 type errors
- [x] 4.3 Manually exercise: register equipment, change its status, register a material, list both from a non-privileged member account and confirm read access but blocked writes — exercised end-to-end against a live `pantheon-service`: SITE_FOREMAN member registers equipment (201), plain member blocked (403) but can list (200), admin updates equipment status, admin registers material, plain member blocked from registering (403) but can list (200)
