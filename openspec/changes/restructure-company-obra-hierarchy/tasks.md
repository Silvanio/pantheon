## 1. Prerequisite

- [ ] 1.1 Confirm `add-team-member-invitations` is archived/synced (or archive it as part of this change) so `MembershipInvitation`, `INVITED` status, and the invitation endpoints exist as the base this change extends

## 2. Data model & migrations (pantheon-service)

- [ ] 2.1 `V__rename_project_to_company.sql`: rename table `project` → `company`, entity `Project` → `Company`; rename `project_membership` → `company_membership`; drop construction-function/specialty columns from `company_membership`
- [ ] 2.2 `V__create_plan_table.sql`: create `plan` (id, code, name, active_site_limit, sort_order), seed Basic (2), Profissional (10), Ilimitado (null); add `company.plan_id` FK; migrate/drop the old plan enum column
- [ ] 2.3 `V__add_company_profile_fields.sql`: add/rename `legal_name`, `trade_name`, `cnpj`, `address`, `logo_object_key` on `company`
- [ ] 2.4 `V__rename_construction_site_company_fk.sql`: rename `construction_site.project_id` → `company_id`; add optional `photo_object_key`
- [ ] 2.5 `V__create_site_membership.sql`: create `site_membership` (id, construction_site_id, user_id nullable, function [CLIENT, ARCHITECT, ENGINEER, SITE_FOREMAN, SERVICE_PROVIDER], service_provider_trade nullable, status, client_cpf nullable, display_name, contact_email)
- [ ] 2.6 `V__alter_membership_invitation_polymorphic.sql`: add `membership_type` (`COMPANY`/`SITE`) to `membership_invitation`; drop the FK on `membership_id`, keep it as an indexed UUID
- [ ] 2.7 `V__drop_architectural_project.sql` / `V__create_site_document_project.sql`: drop `architectural_project`; create `site_document_project` and `site_document_project_attachment`
- [ ] 2.8 `V__create_site_permission_override.sql`: create `site_permission_override` (id, construction_site_id, site_membership_id nullable, function nullable, capability, access_level) with a check constraint enforcing exactly one of `site_membership_id`/`function`
- [ ] 2.9 `V__create_orcamento_tables.sql`: create `orcamento`, `orcamento_line_item`, `orcamento_attachment`; `V__create_receipt_verification_photo.sql`: create `receipt_verification_photo`
- [ ] 2.10 Update all JPA entities/enums for the above (`Company`, `CompanyMembership`, `SiteMembership`, `Plan`, `SiteDocumentProject`, `SiteDocumentProjectAttachment`, `SitePermissionOverride`, `Orcamento`, `OrcamentoLineItem`, `OrcamentoAttachment`, `ReceiptVerificationPhoto`) and their repositories

## 3. Company & plan domain logic

- [ ] 3.1 Rewrite `ProjectService`/`ProjectController` → `CompanyService`/`CompanyController`: name-only creation, admin membership, no plan/profile bundling
- [ ] 3.2 Add `PlanService`: list catalog, select plan (blocks if already set — use change, not select, once set), change plan (blocks downgrade below current active-site count)
- [ ] 3.3 Add `Company.onboardingStatus` computation (`PLAN_PENDING`/`PROFILE_PENDING`/`COMPLETE`) and update `GET /api/onboarding/status` to report it per company
- [ ] 3.4 Add company profile completion endpoint (legal name, trade name, CNPJ, address, logo upload to object storage)
- [ ] 3.5 Remove old trial-period fields/logic and the old plan-confirmation endpoint

## 4. Construction site (obra) domain logic

- [ ] 4.1 Update `ConstructionSiteService` to key off `company_id`, enforce admin-of-company authorization, and enforce the plan's active-site limit on creation
- [ ] 4.2 Add optional photo upload on construction site creation/edit (object storage)

## 5. Site membership, permissions & invitations

- [ ] 5.1 Extend `InvitationService`/`MembershipInvitation` handling to branch on `membership_type` (`COMPANY` vs `SITE`), resolving/flipping the correct table on accept/complete-registration
- [ ] 5.2 Add `SiteMembershipService`: create client/architect/engineer/site-foreman invitations (always `INVITED`), create service-provider memberships (accountless by default, `ACTIVE`, no invitation), attach an account to an existing accountless service-provider membership later
- [ ] 5.3 Add `SiteAccessService`: single authorization entry point resolving "is this user an active member of this site" and "what is their function", used by every site-scoped controller instead of ad hoc checks
- [ ] 5.4 Add `SitePermissionService`: resolve effective access level for (site, membership, capability) — member override → function override → hardcoded default (per design.md Decision 7); admin endpoints to set function-level and member-level overrides
- [ ] 5.5 Wire `SitePermissionService` into daily-report creation, site-document-project creation, equipment/material registration, and material-request creation/approval checks

## 6. Site document projects

- [ ] 6.1 Add `SiteDocumentProjectService`/`Controller`: create (name, site, creator), list, detail
- [ ] 6.2 Add PDF-only attachment upload/download reusing the object-storage pattern from `DailyReportAttachment`

## 7. Materials/budget (orçamento) workflow

- [ ] 7.1 Add `OrcamentoService`: draft against a `MaterialRequest` with priced line items, send to client (`SENT`), client approve/reject (reason required on reject), propagate approval to the request's status
- [ ] 7.2 Add payment-proof / invoice attachment upload on an `Orcamento` (kind `PAYMENT_PROOF` / `INVOICE`)
- [ ] 7.3 Extend `ReceiptVerification` to accept one or more delivery-proof photos (`ReceiptVerificationPhoto`)
- [ ] 7.4 Rewrite existing approve/reject/request endpoints to check `SitePermissionService` (`MATERIAL_REQUEST`/`MATERIAL_APPROVAL`) instead of hardcoded admin/`ENGINEER` checks

## 8. Daily report & equipment/material rescoping

- [ ] 8.1 Update `DailyReportService` authorization to use `SiteAccessService`/`SitePermissionService` (`DAILY_REPORT` capability) instead of project-membership checks
- [ ] 8.2 Update `EquipmentService`/`MaterialService` authorization to use `SitePermissionService` (`EQUIPMENT_MATERIAL` capability) instead of project-admin/`SITE_FOREMAN` checks

## 9. REST surface

- [ ] 9.1 New endpoints: `POST/GET /api/companies`, `GET /api/plans`, `POST /api/companies/{id}/plan`, `PUT /api/companies/{id}/plan`, `PUT /api/companies/{id}/profile`
- [ ] 9.2 New endpoints: `POST/GET /api/sites/{id}/members` (client/architect/engineer/site-foreman/service-provider), `GET /api/sites/{id}/permissions`, `PUT /api/sites/{id}/permissions`
- [ ] 9.3 New endpoints: `POST/GET /api/sites/{id}/projects`, `POST /api/site-projects/{id}/attachments`
- [ ] 9.4 New endpoints: `POST /api/material-requests/{id}/orcamentos`, `POST /api/orcamentos/{id}/send`, `POST /api/orcamentos/{id}/approve`, `POST /api/orcamentos/{id}/reject`, `POST /api/orcamentos/{id}/attachments`, `POST /api/receipt-verifications/{id}/photos`
- [ ] 9.5 Update exception handling for the new 403/404/409 cases introduced above

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

- [ ] 13.1 `pantheon-service` unit/slice tests: company creation, plan selection/change (including downgrade block), onboarding-status computation, site creation blocked by plan limit
- [ ] 13.2 `pantheon-service` tests: site membership invite/accept for each function, accountless service-provider creation and later account attachment
- [ ] 13.3 `pantheon-service` tests: permission resolution (member override > function override > default) and enforcement on daily report / document projects / material request / material approval
- [ ] 13.4 `pantheon-service` tests: orçamento draft → send → client approve/reject, payment-proof/invoice attachments, receipt-verification photos
- [ ] 13.5 `pantheon-web`: `vue-tsc` typecheck + `vite build` pass with all new/changed views and routes
- [ ] 13.6 Manual QA on the full stack via `infra/docker-compose.yml`: sign up, select a plan, complete company profile, create an obra, invite each site role, configure a permission override, run the orçamento flow end to end
- [ ] 13.7 Run `/opsx:sync-specs` (or archive) once implemented and verified, updating `openspec/specs/` accordingly
