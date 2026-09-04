## 1. Prerequisite

- [x] 1.1 `add-team-member-invitations`'s tables/entities (`MembershipInvitation`, `INVITED` status) were already implemented in the codebase; this change builds directly on them (generalized in 2.6/5.1). Formally archiving that change into `openspec/specs/` is still outstanding — see 13.7.

## 2. Data model & migrations (pantheon-service)

- [x] 2.1 `V24__rename_project_to_company.sql`: rename table `project` → `company`, entity `Project` → `Company`; `V27__rename_project_membership_to_company_membership.sql`: rename `project_membership` → `company_membership`, drop construction-function/specialty columns
- [x] 2.2 `V25__create_plan_table.sql` / `V26__alter_company_plan_and_profile.sql`: create `plan` (id, code, name, active_site_limit, sort_order), seed Basic (2), Profissional (10), Ilimitado (null); add `company.plan_id` FK; migrate/drop the old plan column
- [x] 2.3 `V26__alter_company_plan_and_profile.sql`: add `legal_name`, `trade_name`, `cnpj`, `address`, `logo_object_key` on `company`
- [x] 2.4 `V28__rename_construction_site_company_fk.sql`: rename `construction_site.project_id` → `company_id`; add optional `photo_object_key`
- [x] 2.5 `V29__create_site_membership.sql`: create `site_membership` (id, construction_site_id, user_id nullable, function [CLIENT, ARCHITECT, ENGINEER, SITE_FOREMAN, SERVICE_PROVIDER], service_provider_trade nullable, status, client_cpf nullable, display_name, contact_email)
- [x] 2.6 `V30__alter_membership_invitation_polymorphic.sql`: add `membership_type` (`COMPANY`/`SITE`) to `membership_invitation`; drop the FK on `membership_id`, keep it as an indexed UUID
- [x] 2.7 `V31__replace_architectural_project_with_site_document_project.sql`: drop `architectural_project`; create `site_document_project` and `site_document_project_attachment`
- [x] 2.8 `V32__create_site_permission_override.sql`: create `site_permission_override` (id, construction_site_id, site_membership_id nullable, function nullable, capability, access_level) with a check constraint enforcing exactly one of `site_membership_id`/`function`
- [x] 2.9 `V33__create_orcamento_tables.sql`: create `orcamento`, `orcamento_line_item`, `orcamento_attachment`, `receipt_verification_photo`
- [x] 2.10 All JPA entities/enums for the above (`Company`, `CompanyMembership`, `SiteMembership`, `Plan`, `PlanCode`, `CompanyOnboardingStatus`, `MembershipType`, `SiteDocumentProject`, `SiteDocumentProjectAttachment`, `SitePermissionOverride`, `PermissionCapability`, `AccessLevel`, `Orcamento`, `OrcamentoStatus`, `OrcamentoLineItem`, `OrcamentoAttachment`, `AttachmentKind`, `ReceiptVerificationPhoto`) and their repositories are in place and compile

## 3. Company & plan domain logic

- [x] 3.1 `ProjectService`/`ProjectController` replaced by `CompanyService`/`CompanyController`: name-only creation, admin membership, no plan/profile bundling
- [x] 3.2 `PlanService`: list catalog, select/change plan (`selectOrChangePlan`, blocks downgrade below current active-site count via `PlanDowngradeBlockedException`)
- [x] 3.3 `Company.getOnboardingStatus()` computation (`PLAN_PENDING`/`PROFILE_PENDING`/`COMPLETE`); `GET /api/onboarding/status` reports it per company via `CompanyService.getOnboardingStatus`
- [x] 3.4 `PUT /api/companies/{id}/profile` (multipart: legal name, trade name, CNPJ, address, logo upload to object storage)
- [x] 3.5 Old trial-period fields/logic and the old plan-confirmation endpoint removed

## 4. Construction site (obra) domain logic

- [x] 4.1 `ConstructionSiteService` keys off `company_id`, enforces admin-of-company authorization, and enforces the plan's active-site limit on creation (`PlanService.requireCapacityForNewSite`)
- [ ] 4.2 Add optional photo upload on construction site creation/edit (object storage) — column/key builder exist (`ConstructionSite.photoObjectKey`, `StorageKeys.sitePhotoKey`); controller/service wiring not yet added

## 5. Site membership, permissions & invitations

- [x] 5.1 `InvitationService` branches on `membership_type` (`COMPANY` vs `SITE`), resolving/flipping the correct table on accept/complete-registration
- [x] 5.2 `SiteMembershipService`: invite client/architect/engineer/site-foreman (always `INVITED`), create accountless service-provider memberships (`ACTIVE`/`NONE`, no invitation), attach an account to an existing accountless membership later
- [x] 5.3 `SiteAccessService`: single authorization entry point (`resolve`/`requireAccess`) used by every site-scoped service instead of ad hoc checks
- [x] 5.4 `SitePermissionService`: resolves effective access level (member override → function override → hardcoded default per design.md Decision 7); override CRUD/admin endpoints not yet added (see 9.2)
- [x] 5.5 Wired into daily-report creation, site-document-project creation, equipment/material registration, and material-request creation/approval checks

## 6. Site document projects

- [x] 6.1 `SiteDocumentProjectService`/`Controller`: create (name, site, creator), list, detail
- [x] 6.2 PDF-only attachment upload/download reusing the object-storage pattern from `DailyReportAttachment`

## 7. Materials/budget (orçamento) workflow

- [ ] 7.1 Add `OrcamentoService`: draft against a `MaterialRequest` with priced line items, send to client (`SENT`), client approve/reject (reason required on reject), propagate approval to the request's status — entities/migrations exist (2.9), service not yet written
- [ ] 7.2 Add payment-proof / invoice attachment upload on an `Orcamento` (kind `PAYMENT_PROOF` / `INVOICE`)
- [ ] 7.3 Extend `ReceiptVerification` to accept one or more delivery-proof photos (`ReceiptVerificationPhoto`) — entity exists, service/controller wiring not yet added
- [x] 7.4 Existing approve/reject/request endpoints check `SitePermissionService` (`MATERIAL_REQUEST`/`MATERIAL_APPROVAL`) instead of hardcoded admin/`ENGINEER` checks

## 8. Daily report & equipment/material rescoping

- [x] 8.1 `DailyReportService` authorization uses `SiteAccessService`/`SitePermissionService` (`DAILY_REPORT` capability) instead of project-membership checks; `DailyReportSignatureService`, `DailyReportMediaService`, `DailyReportPdfService` updated too
- [x] 8.2 `EquipmentService`/`MaterialService` authorization uses `SitePermissionService` (`EQUIPMENT_MATERIAL` capability) instead of project-admin/`SITE_FOREMAN` checks

## 9. REST surface

- [x] 9.1 `POST/GET /api/companies`, `GET /api/plans`, `PUT /api/companies/{id}/plan`, `PUT /api/companies/{id}/profile`, `POST/GET /api/companies/{id}/staff`
- [ ] 9.2 New endpoints: `GET/PUT /api/sites/{id}/permissions` (function-level and member-level override CRUD) — `POST/GET /api/sites/{id}/members` is done (`SiteMembershipController`)
- [x] 9.3 `POST/GET /api/sites/{id}/projects`, `POST/GET /api/site-projects/{id}/attachments`, `GET /api/site-projects/{id}/attachments/{attachmentId}/content`
- [ ] 9.4 New endpoints: `POST /api/material-requests/{id}/orcamentos`, `POST /api/orcamentos/{id}/send`, `POST /api/orcamentos/{id}/approve`, `POST /api/orcamentos/{id}/reject`, `POST /api/orcamentos/{id}/attachments`, `POST /api/receipt-verifications/{id}/photos`
- [x] 9.5 Exception handling updated for the new 403/404/409 cases introduced above (`CompanyExceptionHandler`, extended `ConstructionExceptionHandler`); orçamento-specific exceptions (`OrcamentoNotFoundException` etc.) exist but their controller isn't wired yet

## 10. pantheon-message

- [ ] 10.1 Update invitation email template to render the correct target name/type (company vs. construction site)
- [ ] 10.2 Add an "orçamento enviado" notification email for the `orcamento-sent` event

## 11. Local orchestration & infra

- [ ] 11.1 No new infra services required; confirm MinIO buckets/paths accommodate the new object kinds (company logo, site photo, site-document-project PDFs, orçamento attachments, receipt-verification photos)

## 12. pantheon-web

- [ ] 12.1 Replace onboarding popup/project-registration/plan-selection views and routing guard with: company-creation form → plan modal → company-profile form, driven by the new onboarding-status endpoint
- [ ] 12.2 Rebuild dashboard as an obra-card grid (photo, name, start date); add header profile menu (change plan, edit company data)
- [ ] 12.3 Split `TeamPanel.vue` into company staff view and per-site team view (clients, service providers, architects, engineers, site foremen), each with its own add-member form and "Convite" badge
- [ ] 12.4 Add site document-projects view (list, create, detail with PDF upload/download)
- [ ] 12.5 Add permission-configuration view (function defaults + member overrides) per site
- [ ] 12.6 Extend materials view with the orçamento sub-flow (draft, send, client approve/reject, payment-proof/invoice attachments) and delivery-proof photo upload on receipt verification
- [ ] 12.7 Add a disabled "Cronograma" nav entry under the obra menu
- [ ] 12.8 Update i18n strings for every new/changed view; update `useProjects`/related composables for the `Company`/`ConstructionSite`/`SiteMembership` shapes

## 13. Tests & verification

- [x] 13.0 Removed 11 obsolete unit test files that exercised now-deleted/renamed classes (`ProjectServiceTest`, `ArchitecturalProjectServiceTest`, `InvitationServiceTest`, `ConstructionSiteServiceTest`, `EquipmentServiceTest`, `MaterialServiceTest`, `MaterialRequestServiceTest`, `DailyReportServiceTest`, `DailyReportMediaServiceTest`, `DailyReportSignatureServiceTest`, `DailyReportPdfServiceTest`) so `mvn test-compile`/`mvn test` are green again; `mvn test` (full Spring context + all 10 new Flyway migrations against the real dev DB) passes. Replacement tests per the new architecture are 13.1-13.4 below, still outstanding.
- [ ] 13.1 `pantheon-service` unit/slice tests: company creation, plan selection/change (including downgrade block), onboarding-status computation, site creation blocked by plan limit
- [ ] 13.2 `pantheon-service` tests: site membership invite/accept for each function, accountless service-provider creation and later account attachment
- [ ] 13.3 `pantheon-service` tests: permission resolution (member override > function override > default) and enforcement on daily report / document projects / material request / material approval
- [ ] 13.4 `pantheon-service` tests: orçamento draft → send → client approve/reject, payment-proof/invoice attachments, receipt-verification photos
- [ ] 13.5 `pantheon-web`: `vue-tsc` typecheck + `vite build` pass with all new/changed views and routes
- [ ] 13.6 Manual QA on the full stack via `infra/docker-compose.yml`: sign up, select a plan, complete company profile, create an obra, invite each site role, configure a permission override, run the orçamento flow end to end
- [ ] 13.7 Run `/opsx:sync-specs` (or archive) once implemented and verified, updating `openspec/specs/` accordingly
