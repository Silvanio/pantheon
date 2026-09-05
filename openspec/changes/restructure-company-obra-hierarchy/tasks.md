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
- [x] 4.2 `PUT /api/construction-sites/{id}/photo` (multipart) uploads to object storage and calls `ConstructionSiteService.updatePhoto`

## 5. Site membership, permissions & invitations

- [x] 5.1 `InvitationService` branches on `membership_type` (`COMPANY` vs `SITE`), resolving/flipping the correct table on accept/complete-registration
- [x] 5.2 `SiteMembershipService`: invite client/architect/engineer/site-foreman (always `INVITED`), create accountless service-provider memberships (`ACTIVE`/`NONE`, no invitation), attach an account to an existing accountless membership later
- [x] 5.3 `SiteAccessService`: single authorization entry point (`resolve`/`requireAccess`) used by every site-scoped service instead of ad hoc checks
- [x] 5.4 `SitePermissionService`: resolves effective access level (member override → function override → hardcoded default per design.md Decision 7); `setFunctionOverride`/`setMemberOverride` + `SitePermissionController` (company-staff-only) added
- [x] 5.5 Wired into daily-report creation, site-document-project creation, equipment/material registration, and material-request creation/approval checks

## 6. Site document projects

- [x] 6.1 `SiteDocumentProjectService`/`Controller`: create (name, site, creator), list, detail
- [x] 6.2 PDF-only attachment upload/download reusing the object-storage pattern from `DailyReportAttachment`

## 7. Materials/budget (orçamento) workflow

- [x] 7.1 `OrcamentoService`: draft against a `MaterialRequest` with priced line items, send to client (`SENT`), client approve/reject (reason required on reject, requires an active `CLIENT` `SiteMembership`), propagates approval to the request's status
- [x] 7.2 Payment-proof / invoice attachment upload on an `Orcamento` (kind `PAYMENT_PROOF` / `INVOICE`) via `OrcamentoController`
- [x] 7.3 `MaterialRequestService.uploadVerificationPhoto`/`listVerificationPhotos` accept one or more delivery-proof photos (`ReceiptVerificationPhoto`) against an existing `ReceiptVerification`
- [x] 7.4 Existing approve/reject/request endpoints check `SitePermissionService` (`MATERIAL_REQUEST`/`MATERIAL_APPROVAL`) instead of hardcoded admin/`ENGINEER` checks

## 8. Daily report & equipment/material rescoping

- [x] 8.1 `DailyReportService` authorization uses `SiteAccessService`/`SitePermissionService` (`DAILY_REPORT` capability) instead of project-membership checks; `DailyReportSignatureService`, `DailyReportMediaService`, `DailyReportPdfService` updated too
- [x] 8.2 `EquipmentService`/`MaterialService` authorization uses `SitePermissionService` (`EQUIPMENT_MATERIAL` capability) instead of project-admin/`SITE_FOREMAN` checks

## 9. REST surface

- [x] 9.1 `POST/GET /api/companies`, `GET /api/plans`, `PUT /api/companies/{id}/plan`, `PUT /api/companies/{id}/profile`, `POST/GET /api/companies/{id}/staff`
- [x] 9.2 `POST/GET /api/sites/{id}/members` (`SiteMembershipController`); `GET /api/sites/{id}/permissions`, `PUT /api/sites/{id}/permissions/function`, `PUT /api/sites/{id}/permissions/member` (`SitePermissionController`)
- [x] 9.3 `POST/GET /api/sites/{id}/projects`, `POST/GET /api/site-projects/{id}/attachments`, `GET /api/site-projects/{id}/attachments/{attachmentId}/content`
- [x] 9.4 `POST/GET /api/material-requests/{id}/orcamentos`, `GET /api/orcamentos/{id}`, `POST /api/orcamentos/{id}/send`, `POST /api/orcamentos/{id}/approve`, `POST /api/orcamentos/{id}/reject`, `POST /api/orcamentos/{id}/attachments`, `GET /api/orcamento-attachments/{id}/content`, `POST /api/material-requests/{id}/receipt-verifications/{verificationId}/photos`
- [x] 9.5 Exception handling updated for the new 403/404/409 cases (`CompanyExceptionHandler`, extended `ConstructionExceptionHandler` covering orçamento/site-document-project exceptions too)

## 10. pantheon-message

- [x] 10.1 `InvitationEmailHandler` renders the correct target name/type (company vs. construction site) from the generalized `targetId`/`targetName`/`membershipType` event fields
- [x] 10.2 `OrcamentoSentEmailHandler` sends an "orçamento enviado" notification for the `orcamento-sent` event, published by `OrcamentoService.send` to every active client `SiteMembership` on the site

## 11. Local orchestration & infra

- [x] 11.1 No new infra services required; MinIO storage confirmed working live for the new object kinds (company logo, site photo, site-document-project PDF, all exercised in the 13.6 manual QA pass)

## 12. pantheon-web

- [x] 12.1 Onboarding popup/project-registration/plan-selection views and routing guard replaced with `CompanyCreationView` → `PlanSelectionView` (real catalog from `GET /api/plans`) → `CompanyProfileView`, driven by `useCompanyOnboarding`/`GET /api/onboarding/status`; router guard also handles a 401 mid-check by logging out instead of crashing navigation (bug found and fixed during manual QA)
- [x] 12.2 `DashboardView` rebuilt as an obra-card grid (`SitePhoto` + name + start date); header profile menu (`CompanyLogo` + "Perfil" dropdown: change plan, edit company data, sair)
- [x] 12.3 `TeamPanel.vue` replaced by `CompanyStaffPanel.vue` (company staff, on `CompanySettingsView`) and `SiteTeamPanel.vue` (per-site: client/architect/engineer/site-foreman invited, service-provider accountless-by-default), each with its own add-member form and "Convite" badge
- [x] 12.4 `SiteDocumentProjectsPanel.vue`: list, create, expand-to-detail with PDF-only upload/download
- [x] 12.5 `SitePermissionsPanel.vue`: function × capability grid with live-saved access-level overrides
- [x] 12.6 `MaterialRequestDetailView.vue` extended with the orçamento sub-flow (draft with priced items, send, client approve/reject, payment-proof/invoice attachments) and delivery-proof photo upload on receipt verification
- [x] 12.7 Disabled "Cronograma" tab added to `SiteDetailView`'s nav
- [x] 12.8 i18n strings added/updated for every new/changed view (`pt-BR.json`); `useProjects.ts` replaced by `useCompanies`, `useConstructionSites`, `useSiteMembers`, `useSitePermissions`, `useSiteDocumentProjects`, `useEquipmentMaterials`; `useMaterialRequests` extended with orçamento calls; `useProjectOnboarding` replaced by `useCompanyOnboarding`; `useInvitations` updated for generalized `targetName`/`membershipType`

## 13. Tests & verification

- [x] 13.0 Removed 11 obsolete unit test files that exercised now-deleted/renamed classes (`ProjectServiceTest`, `ArchitecturalProjectServiceTest`, `InvitationServiceTest`, `ConstructionSiteServiceTest`, `EquipmentServiceTest`, `MaterialServiceTest`, `MaterialRequestServiceTest`, `DailyReportServiceTest`, `DailyReportMediaServiceTest`, `DailyReportSignatureServiceTest`, `DailyReportPdfServiceTest`) so `mvn test-compile`/`mvn test` are green again; `mvn test` (full Spring context + all 33 Flyway migrations against the real dev DB) passes. Replacement tests per the new architecture are 13.1-13.4 below, still outstanding.
- [x] 13.1 `pantheon-service` unit/slice tests: company creation, plan selection/change (including downgrade block), onboarding-status computation, site creation blocked by plan limit (`CompanyServiceTest`, `PlanServiceTest`)
- [x] 13.2 `pantheon-service` tests: site membership invite/accept for each function, accountless service-provider creation and later account attachment (`SiteMembershipServiceTest`, `SiteAccessServiceTest`)
- [x] 13.3 `pantheon-service` tests: permission resolution (member override > function override > default) and enforcement on daily report / document projects / material request / material approval (`SitePermissionServiceTest`)
- [x] 13.4 `pantheon-service` tests: orçamento draft → send → client approve/reject, payment-proof/invoice attachments, receipt-verification photos (`OrcamentoServiceTest`); all 42 tests across 9 classes pass (`mvn -q -o test`)
- [x] 13.5 `pantheon-web`: `vue-tsc -b` and `vite build` both pass clean (verified with `--force` to bypass incremental cache)
- [x] 13.6 Manual QA performed live in a real browser against the full stack (pantheon-service + pantheon-web + Postgres/RabbitMQ/MinIO via `infra/docker-compose.yml`): registered a user, created a company, selected the Profissional plan, completed the company profile (with logo upload), reached the dashboard, created an obra (with card rendering), opened the obra detail, created a site document project and uploaded a PDF to it, edited a permission override on the Permissões tab (confirmed persisted after reload), and added an accountless service-provider site member. Additionally covered in a second pass: full invitation-email round trip (invited a client via email, confirmed the email in Mailpit, completed registration, accepted the invite, confirmed `SiteMembership` became `ACTIVE`) and the full orçamento workflow (registered a material, created a material request, drafted an orçamento with a priced line item, sent it to the client, confirmed the status transitioned to `SENT` and the "orçamento enviado" notification email reached the client in Mailpit). This pass found and fixed three bugs: (1) an unhandled 401 during the onboarding-status check crashed router navigation instead of logging out — fixed in the router guard; (2) a Portuguese grammar error in the invitation email ("participar de a obra" instead of "participar da obra") — fixed in `InvitationEmailHandler` and re-verified live after restarting pantheon-message; (3) a routing bug where a site-only user (e.g. a client with no company of their own) was incorrectly forced into company-creation onboarding — fixed by adding `siteIds` to `OnboardingStatusResponse`/`GET /api/onboarding/status` and updating the router guard to route such users to their obra instead.
- [ ] 13.7 Run `/opsx:sync-specs` (or archive) once implemented and verified, updating `openspec/specs/` accordingly
