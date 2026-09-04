## Context

Today `pantheon-service` has one top-level tenant entity, `Project` (table `project`), created by a user who becomes its `ADMIN`. A `Project` has a trial period, an optional confirmed `plan` (Basic/Pro/Ilimitado, hardcoded), and directly owns three unrelated things: `ConstructionSite`s, `ArchitecturalProject`s, and a flat `ProjectMembership` list (role `ADMIN`/`MEMBER` plus an optional construction function: `CLIENT`, `ENGINEER`, `ARCHITECT`, `SITE_FOREMAN`, `SERVICE_PROVIDER`, `OTHER`). Daily reports, equipment, materials, and material requests all hang off `ConstructionSite`, but authorization for all of them ("is this user a member/admin/engineer/site-foreman") is still resolved through `ProjectMembership` at the `Project` level — every site under a project shares the same team.

That doesn't match the product: a company ("empresa") is the thing that signs up and pays for a plan; a construction site ("obra") is the thing that actually has a team (a client, architects, engineers, service providers), its own document projects, its own daily reports, and its own materials/budget workflow. Two different obras of the same company can — and normally do — have entirely different teams. Separately, `pantheon-web`'s onboarding today shows a reactive "create or join a project" popup and only forces plan selection once a trial/plan has already expired, whereas the intended flow is: sign up → **must** choose a plan → complete the company's commercial profile → dashboard.

A separate, still-open change (`add-team-member-invitations`) added the `INVITED`/pre-registration invitation lifecycle on top of `ProjectMembership` (its tasks are implemented but the change has not yet been archived/synced into `openspec/specs/`). This change builds directly on that lifecycle rather than re-deriving it — see Migration Plan.

## Goals / Non-Goals

**Goals:**
- Replace `Project` with `Company` as the tenant entity; `ConstructionSite` ("Obra") belongs to `Company`, not the other way conceptually.
- Give each obra its own team (`SiteMembership`), independent of company staff (`CompanyMembership`), with client/architect/engineer invite semantics and optional-account service providers.
- Move document projects (replacing `ArchitecturalProject`) and the materials/budget workflow to be owned by the obra.
- Reorder onboarding: account → mandatory plan selection → company profile → dashboard; make plans a registered catalog with a per-plan obra limit.
- Add a per-obra permission configuration layer (create vs. view, request vs. approve) that authorization checks consult.
- Extend the materials workflow with a client-facing budget ("orçamento") lifecycle and delivery-proof photos.

**Non-Goals:**
- Billing/payment processing for plans (selection only, as today).
- Cronograma implementation (placeholder menu entry only).
- Data backfill/migration *scripts* for existing production rows — there is no production data yet, so migrations only need to work against an empty/dev database.
- Changing how Google OAuth provisioning or the base session-token format work.
- Revisiting the invitation token/email mechanics themselves (reused as-is from `add-team-member-invitations`).

## Decisions

### 1. `Project` → `Company`; `ConstructionSite` gets a direct `company_id`

Rename table `project` → `company` (and entity `Project` → `Company`); rename `project_membership` → `company_membership` (see Decision 3). `ConstructionSite.project_id` is renamed to `company_id`, still `NOT NULL`, still admin-created.

- **Why a rename over a new parallel table:** `Company` *is* semantically what `Project` already models (a paying tenant with an admin, a plan, a trial) — there is no case where both concepts need to coexist. A rename preserves the existing admin/trial/plan machinery instead of duplicating it.
- **Alternative considered:** introduce `Company` as a brand-new entity and keep `Project` as an obsolete pass-through. Rejected — doubles the entities needing security checks for no benefit, since nothing wants both.

### 2. `ArchitecturalProject` is dropped; site document projects belong to the obra

`ArchitecturalProject` (and its "optionally linked to a site" relationship) is removed. New `SiteDocumentProject` (table `site_document_project`): `id`, `construction_site_id NOT NULL`, `name`, `created_at`, `created_by` (FK to whichever membership created it — see Decision 3), plus `SiteDocumentProjectAttachment` (`id`, `site_document_project_id`, object-storage key, original filename, content type constrained to `application/pdf`) following the existing `daily-report-media-and-signoff` object-storage pattern (files in MinIO, never as DB blobs).

- **Why drop the "optionally linked to a site" indirection:** the whole point of this change is that a document project is a thing the obra has, not a thing the company has that might float unattached. Making `construction_site_id` mandatory removes an entire branch of null-handling in every consumer.
- **Alternative considered:** keep `ArchitecturalProject` schema and just add a mandatory site FK. Rejected — the entity is being generalized past "architectural" (any PDF-bearing project), so a rename avoids a misleading name surviving the redesign.

### 3. Team splits into `CompanyMembership` (staff) and `SiteMembership` (obra team)

`project_membership` is renamed `company_membership` and *narrowed*: `id`, `company_id`, `user_id`, `role` (`ADMIN`/`MEMBER`), `status` (`INVITED`/`ACTIVE`, from the invitations change), no construction function. This is the company's internal staff list — the people who work for the company itself, not for a specific obra.

New `site_membership`: `id`, `construction_site_id NOT NULL`, `user_id` (nullable — see below), `function` (`CLIENT`, `ARCHITECT`, `ENGINEER`, `SITE_FOREMAN`, `SERVICE_PROVIDER`), `service_provider_trade` (nullable text, only meaningful when `function = SERVICE_PROVIDER`; free text so new trades don't need a migration — Pedreiro, Encanador, Eletricista, Terceirizado are UI-suggested values, not an enum), `status` (`INVITED`/`ACTIVE`/`NONE`, see below), `client_cpf` (nullable, only for `CLIENT`), `display_name` / `contact_email` (nullable, used when `user_id` is null).

- **Why `SITE_FOREMAN` (Mestre de Obra) stays a first-class function instead of a `SERVICE_PROVIDER` trade:** the user's own grouping mentions it alongside the informal trades, but functionally it retains today's elevated authority (registering equipment/materials) — exercising that authority requires an account and login, unlike the other, registration-optional trades. Keeping it a distinct `function` (invite-required, like architect/engineer) rather than a free-text trade preserves that authority as a first-class, checkable value instead of pattern-matching a free-text field.

- **Why two separate tables instead of one wide membership table with a "scope" column:** a company admin is never also "the engineer on obra X" in the same row — the two are different relationships with different lifecycles (a `CompanyMembership` never needs a trade sub-type; a `SiteMembership` never needs `role=ADMIN`). Keeping them separate keeps each table's columns meaningful for every row, mirroring the reasoning already used in this codebase for keeping invitation metadata out of the core membership row.
- **Service providers without an account:** `user_id` is nullable on `site_membership`; when absent, `display_name`/`contact_email` (free text, no login) hold the identity, and `status = NONE` (no invitation, no login gating). Registration stays *optional* for this function: an admin can later attach a real `user_id` and move the row to the invite flow if the provider wants an account.
- **Client/Architect/Engineer always require the invite lifecycle:** `status` starts `INVITED` (mirroring `CompanyMembership`); the accept step is identical to today's mechanism, just targeting a `SiteMembership` row instead of a `ProjectMembership` row.
- **Alternative considered:** a single polymorphic `membership` table with a `scope_type` (`COMPANY`/`SITE`) and a nullable `scope_id`. Rejected — every query would need a scope-type filter and the columns that are meaningless per scope (trade, CPF, role) would still coexist, which is exactly what decision 3's split avoids.

### 4. Invitations generalize to target either membership table

`membership_invitation` (from `add-team-member-invitations`) currently has a `membership_id UNIQUE REFERENCES project_membership(id)`. It gains a `membership_type VARCHAR(20)` (`COMPANY`/`SITE`) and `membership_id` becomes a plain UUID (no DB-level FK, resolved in code by type) so one invitation table keeps serving both `CompanyMembership` and `SiteMembership` invites. `InvitationService.accept`/`completeRegistration` branch on `membership_type` to flip the right table's `status`.

- **Why not two invitation tables:** the token/email/accept mechanics are identical regardless of target; duplicating the table (and `InvitationService`) would just be copy-paste with no behavioral difference.
- **Alternative considered:** keep the FK and add a second `site_membership_id` nullable column (exactly one of the two set). Rejected — every reader would need an `exactly-one-of` check; a discriminator is simpler and matches how the rest of this design already uses `function`/`membership_type` discriminators.

### 5. Plans become a registered catalog with an obra limit

New `plan` table: `id`, `code` (`BASIC`/`PROFISSIONAL`/`ILIMITADO`), `name`, `active_site_limit` (nullable = unlimited; seeded 2 / 10 / null), `sort_order`. `company.plan_id` (nullable FK, set once a plan is chosen) replaces the old plan enum column. The existing 3-day trial concept is dropped from the *login-blocking* path (see Decision 6) but the column/history can remain for reporting; enforcement moves to "has the company chosen a plan yet."

- **Why a table over keeping an enum:** the request explicitly asks for plans to be "registered" data, and a table lets the limit (and future price/feature flags) change without a code deploy — consistent with how every other catalog in this codebase (equipment, materials) is a DB row, not an enum.
- **Site-count limit replaces project-count limit:** the old limit ("Basic: 2 projects, Pro: 10") capped how many `Project`s a user administered; since `Project` is now `Company` (the tenant itself, created once), the equivalent scarcity in the new model is *how many obras a company runs*, so the same numbers now cap `active_site_limit` per company. A company administering only ever one company is the common case and is not newly restricted.
- **Alternative considered:** keep limiting company count per admin user. Rejected as not matching the product's actual constraint (a company doesn't want to be told it can only run 2 *companies* — it wants to run 2 *obras*).

### 6. Onboarding order: account → plan modal → company profile → dashboard

`Company.onboardingStatus` (derived, not stored) is computed as `PLAN_PENDING` (no `plan_id`), else `PROFILE_PENDING` (missing any of legal name/trade name/CNPJ/address/logo), else `COMPLETE`. `GET /api/onboarding/status` returns this per company the user administers; `pantheon-web`'s router guard redirects to the plan modal, then the profile form, then the dashboard, in that order, replacing the old "create or join a project" popup and the reactive plan-expiry screen entirely. A brand-new signup with no company yet is routed straight into "create your company" (name only) → plan modal → profile form, collapsing today's separate "project registration" step.

- **Why compute onboarding status instead of a stored state machine column:** the same two facts (`plan_id` set? profile fields filled?) are already the source of truth and must stay consistent with direct edits (e.g., editing the profile later from the header menu); a derived status can't drift out of sync with the underlying data the way a separately-stored enum could.
- **Company profile fields live on `Company`, not `AppUser`:** the existing `AppUser` profile (CNPJ/CPF, legal name, address, CEP) models a *person's* own data and is kept as-is (still used, e.g., for a client's personal CPF). `Company` gets its own `legal_name` (Razão Social), `trade_name` (Nome Fantasia), `cnpj`, `address`, and `logo_object_key` — a company's commercial identity is distinct from any one person's.
- **Trial period removed as a gate, plan choice becomes mandatory up front:** since a plan must be picked before the company profile step even appears, there is no window where an unplanned company is usable — the 3-day trial's job (letting people explore before committing) is superseded by "no charge yet" plan selection being free.

### 7. Per-obra permission overrides

New `site_permission_override`: `id`, `construction_site_id`, exactly one of `site_membership_id` (a specific person) or `function` (a default for every `SiteMembership` of that function on the site) set, `capability` (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT_MATERIAL`, `MATERIAL_REQUEST`, `MATERIAL_APPROVAL`), `access_level` (`VIEW`, `MANAGE` for the first three; presence/absence of a `MATERIAL_REQUEST`/`MATERIAL_APPROVAL` row simply grants/denies that action, which has no "view" variant). Resolution order: a member-specific row wins; otherwise the function-level default; otherwise a hardcoded fallback default (company staff: `MANAGE` everywhere; `CLIENT`: `VIEW` on daily report and document projects, no material/equipment actions; `ENGINEER`/`ARCHITECT`: `MANAGE` on document projects and daily report, `MATERIAL_REQUEST` allowed, `MATERIAL_APPROVAL` allowed only for `ENGINEER` per the existing `material-request-workflow` rule; `SITE_FOREMAN`: `MANAGE` on `EQUIPMENT_MATERIAL` and daily report (matching today's `SITE_FOREMAN` rights), `VIEW` on document projects; `SERVICE_PROVIDER`: `VIEW` on daily report only) so a freshly created obra behaves exactly like today's role rules without the admin having to configure anything.
Every write-side check in `daily-construction-report`, `site-document-projects`, and the materials/budget flow additionally consults this table (in place of, or alongside, the current hardcoded function checks).

- **Why capability + access_level instead of one flag per feature:** the request lists the same shape twice ("create vs. view" for projects and diário; "who can request vs. approve" for orçamento) — a small fixed vocabulary of capabilities keeps the configuration UI and the authorization check uniform instead of growing a bespoke boolean per feature.
- **Why defaults exist without requiring configuration:** the request's examples ("configurar se o engenheiro pode...") describe *overriding* a sensible default, not configuring from a blank slate for every obra; requiring configuration before any obra is usable would be a regression from today's zero-config role rules.

### 8. Materials workflow gains a client-facing budget ("orçamento")

`material-request-workflow`'s `MaterialRequest` keeps its current meaning ("pedido de orçamento"). New `Orcamento` (table `orcamento`): `id`, `material_request_id`, `status` (`DRAFT`, `SENT`, `APPROVED`, `REJECTED`), `created_by`, `sent_at`, `decided_at`, `rejection_reason` (required when `REJECTED`, mirroring the existing request-rejection rule); `OrcamentoLineItem` mirrors the request's line items with a unit price; `OrcamentoAttachment` (`kind`: `PAYMENT_PROOF` or `INVOICE`) reuses the object-storage attachment pattern. The existing `ReceiptVerification` gains an optional `ReceiptVerificationPhoto` (object storage, `kind = DELIVERY_PROOF`) so "anexar fotos, prova de entrega" is captured on the same record that already tracks divergence.
A request can have more than one `Orcamento` (e.g., a rejected one followed by a revised one); `MaterialRequest.status` transitions to `APPROVED` when its active `Orcamento` is approved by the client (client approval is itself a `SiteMembership` action gated by the new permission table, not a new global role).

- **Why a new entity rather than fields on `MaterialRequest`:** a request can be re-quoted, so the 1-to-many shape (request → orçamentos) has to exist regardless; folding "the current quote" onto the request would still need history for the rejected-then-revised case.
- **Alternative considered:** treat "orçamento sent/approved" as more statuses on `MaterialRequest` itself (as today). Rejected — it can't represent price, line-item pricing, multiple payment/invoice attachments, or a rejected-and-reissued quote without smuggling a whole sub-entity's worth of data onto the request.

## Risks / Trade-offs

- **[Risk] This is a wide rename touching nearly every existing spec and most of `pantheon-service`'s controllers/services.** → No production data exists yet (stated non-goal), so migrations can do straight `ALTER TABLE ... RENAME` / `ADD COLUMN` without backfill scripts; the blast radius is code, not data safety.
- **[Risk] Splitting membership into two tables doubles the "am I allowed to do X" check sites.** → Centralize both checks behind a single authorization helper (e.g., `SiteAccessService`) that first resolves company-staff access (always `MANAGE`) and falls back to `site_membership` + `site_permission_override`, so individual controllers keep calling one method instead of branching on membership type themselves.
- **[Risk] `add-team-member-invitations` is implemented but not yet archived/synced — its spec deltas aren't in `openspec/specs/` yet, so this change's `team-invitations` delta is written against content that isn't formally "current."** → Migration Plan below sequences archiving that change first; if archived out of order, the `team-invitations` delta in this change is additive/compatible either way (it only adds a `membership_type` discriminator and a second target table).
- **[Risk] Collapsing the 3-day trial into "must pick a free plan immediately" changes the first-run experience someone may be relying on for a demo.** → No billing is attached to any plan yet, so "choosing a plan" costs the user nothing; net effect is one extra click, not a new barrier.
- **[Trade-off] Default permission fallbacks duplicate today's hardcoded role rules as data-shaped defaults rather than deleting them outright.** → Necessary so existing behavior doesn't regress for every obra that never touches the new configuration screen.

## Migration Plan

1. **Land `add-team-member-invitations` first** (archive/sync it — `MembershipInvitation`, `INVITED` status, invitation endpoints) if not already merged; this change's invitation work is additive on top of it.
2. `pantheon-service` Flyway migrations, in order:
   - Rename `project` → `company`, `project_membership` → `company_membership`; drop construction-function columns from `company_membership`.
   - Add `plan` table (seed Basic/Profissional/Ilimitado); add `company.plan_id`; drop the old plan enum column (migrate any existing value into the matching `plan_id` row).
   - Add `legal_name`/`trade_name`/`cnpj`/`address`/`logo_object_key` to `company` (legal_name/cnpj/address likely already exist under different names on `project` — rename in place rather than re-adding).
   - Rename `construction_site.project_id` → `company_id`.
   - Create `site_membership`; migrate any existing project-level `CLIENT`/`ENGINEER`/`ARCHITECT`/`SITE_FOREMAN`/`SERVICE_PROVIDER` `company_membership` rows into `site_membership` per-site is **not applicable** (no production data — non-goal), so this is a clean `CREATE TABLE`.
   - Alter `membership_invitation`: add `membership_type`, drop the FK on `membership_id` (keep it as a plain indexed UUID column).
   - Drop `architectural_project`; create `site_document_project` + `site_document_project_attachment`.
   - Create `site_permission_override`.
   - Create `orcamento`, `orcamento_line_item`, `orcamento_attachment`; add `receipt_verification_photo`.
3. Deploy `pantheon-service` with the renamed/split entities, new endpoints (`Orcamento`, `SiteDocumentProject`, `SitePermissionOverride`, obra-scoped membership), and the rewritten onboarding-status computation.
4. Deploy `pantheon-message` with any new obra-invitation / orçamento-notification email templates.
5. Deploy `pantheon-web`: new signup → plan modal → company-profile flow, obra-card dashboard, header profile menu, obra team management (split staff vs. site team UI), document-projects view, extended materials/budget view, permission-configuration view, disabled "Cronograma" nav entry.
6. **Rollback:** since there is no production data to protect, rollback is redeploying the previous service versions against a database snapshot taken before step 2; the renames are not designed to be reversible in place given the scope (this is acceptable per the stated non-goal).

## Open Questions

- Exact wording/order of the plan-selection modal's copy and whether "Ilimitado" needs a distinct visual treatment (pricing/marketing concern, not structural).
- Whether a `SERVICE_PROVIDER` `SiteMembership` that later gets a real account should retroactively go through the invite/accept flow, or be attachable directly as `ACTIVE` since the admin already vouches for them on-site — defaulted to going through invite/accept for consistency with the other invite-required functions; revisit if that proves friction-heavy in practice.
- Whether `site_permission_override`'s function-level defaults should be editable by the company (i.e., "for this obra, all engineers can approve orçamentos") in addition to per-member overrides — the design above supports both, but the v1 UI could ship member-only overrides and add function-level defaults later if that's sufficient for launch.
