## 1. Data model & migrations (pantheon-service)

- [x] 1.1 Flyway `V40`: clear the 2 dev-only test rows and dependents — `DELETE FROM material_delivery_photo`, `material`, `orcamento_approval`, `orcamento_line_item`, `purchase_request_item`, `orcamento` (in that order) — so the new `NOT NULL` columns below need no backfill
- [x] 1.2 Flyway `V41`: create `purchase_request` (id, construction_site_id FK, name, created_by FK, created_at); add `purchase_request_id UUID NOT NULL REFERENCES purchase_request (id)` to `purchase_request_item`; index on `(construction_site_id, created_at)` for date filtering
- [x] 1.3 Flyway `V42`: create `fornecedor` (id, company_id FK, cnpj, name, address nullable, contact_name nullable, contact_phone nullable, created_by FK, created_at); unique index on `(company_id, cnpj)`
- [x] 1.4 Flyway `V43`: add to `orcamento`: `fornecedor_cnpj VARCHAR NOT NULL`, `fornecedor_nome VARCHAR NOT NULL`, `fornecedor_endereco VARCHAR`, `fornecedor_contato_nome VARCHAR`, `fornecedor_contato_telefone VARCHAR`, `source_fornecedor_id UUID REFERENCES fornecedor (id)`, `source_purchase_request_id UUID REFERENCES purchase_request (id)`; index on `(construction_site_id, source_purchase_request_id)` and `(construction_site_id, created_at)`
- [x] 1.5 JPA: new entity `PurchaseRequest` + `PurchaseRequestRepository` (`findByConstructionSiteIdOrderByCreatedAtDesc`, a same-UTC-day count method for name generation, a date-filtered listing method); new entity `Fornecedor` + `FornecedorRepository` (`findByCompanyIdAndCnpj`, `findByCompanyIdAndCnpjStartingWithOrderByNameAsc`)
- [x] 1.6 JPA: `PurchaseRequestItem` gains `purchaseRequestId` (required); `Orcamento` gains `fornecedorCnpj`/`fornecedorNome`/`fornecedorEndereco`/`fornecedorContatoNome`/`fornecedorContatoTelefone`/`sourceFornecedorId`/`sourcePurchaseRequestId` with a constructor taking the supplier snapshot + optional origin ids, and getters for all of them

## 2. Supplier (Fornecedor) domain logic

- [x] 2.1 `FornecedorService.findOrCreate(companyId, actingUserId, FornecedorRequest)`: returns the existing `Fornecedor` for `(companyId, cnpj)` if present (ignoring differences in the other submitted fields), otherwise creates and returns a new one
- [x] 2.2 `FornecedorService.searchByCnpjPrefix(siteId, actingUserId, prefix)`: requires only site access (any active member, no `MANAGE` capability), resolves the site's `companyId`, rejects a prefix shorter than 5 characters, returns matches ordered by name
- [x] 2.3 DTOs: `FornecedorRequest` (cnpj `@NotBlank`, name `@NotBlank`, address/contactName/contactPhone optional), `FornecedorResponse` (id, cnpj, name, address, contactName, contactPhone)
- [x] 2.4 `FornecedorController`: `GET /api/construction-sites/{siteId}/fornecedores?cnpjPrefix=`

## 3. Pedido de Compra header domain logic

- [x] 3.1 Rename/extend `PurchaseRequestItemService` (or introduce `PurchaseRequestService` alongside it) to add `createPurchaseRequest(siteId, actingUserId, List<PurchaseRequestItemCreationRequest> items)` (requires `PURCHASE_REQUEST` manage): generates the name `"Pedido " + dd/MM/yyyy + " #" + (countThatDay + 1)` (UTC calendar day), persists the `PurchaseRequest`, then persists each item linked to it (still also carrying `constructionSiteId` denormalized)
- [x] 3.2 `listPurchaseRequests(siteId, actingUserId, LocalDate dateFilter)` (any member) and `listItems(purchaseRequestId, actingUserId, statusFilter)` (any member, resolves site via the header)
- [x] 3.3 `convertToOrcamento(siteId, actingUserId, purchaseRequestId, itemIds, FornecedorRequest fornecedor)` (requires `PURCHASE_REQUEST` manage): validates every id in `itemIds` belongs to `purchaseRequestId` and is `PENDING` (rejects otherwise, including any id from a different header), delegates Orçamento creation to `OrcamentoService.createFromPurchaseRequestItems(..., fornecedor)`, marks only the selected items `CONVERTED`, leaves the rest `PENDING` on the same header
- [x] 3.4 Exceptions: `PurchaseRequestNotFoundException`, and extend the existing already-converted exception's usage; a new `ItemsSpanMultiplePurchaseRequestsException` (or reuse a generic `IllegalArgumentException` — pick whichever matches this codebase's existing exception-per-case convention) for the cross-header rejection
- [x] 3.5 `PurchaseRequestController`: `POST /api/construction-sites/{siteId}/purchase-requests`, `GET /api/construction-sites/{siteId}/purchase-requests?date=`, `GET /api/purchase-requests/{id}` (header + items), `POST /api/purchase-requests/{id}/convert-to-orcamento`

## 4. Orçamento domain logic changes

- [x] 4.1 `OrcamentoService.create(siteId, actingUserId, items, FornecedorRequest fornecedor)`: resolves the site's `companyId`, calls `FornecedorService.findOrCreate`, persists the Orçamento with the resolved supplier's fields snapshotted plus `sourceFornecedorId`, `sourcePurchaseRequestId = null`
- [x] 4.2 `OrcamentoService.createFromPurchaseRequestItems(siteId, actingUserId, items, purchaseRequestId, FornecedorRequest fornecedor)`: same supplier resolution, sets `sourcePurchaseRequestId = purchaseRequestId`
- [x] 4.3 Reject Orçamento creation when `fornecedor.cnpj()` or `fornecedor.name()` is blank (bean validation via `@Valid` on the request DTO covers this)
- [x] 4.4 `OrcamentoService.list(siteId, actingUserId, LocalDate dateFilter, UUID purchaseRequestIdFilter)`: both filters optional and combinable
- [x] 4.5 `OrcamentoResponse`/`OrcamentoDetailResponse` include the supplier snapshot fields and `sourcePurchaseRequestId` (plus the originating Pedido de Compra's name, resolved for display convenience)
- [x] 4.6 `OrcamentoCreationRequest` gains a required `FornecedorRequest fornecedor` field; `ConvertPurchaseRequestItemsRequest` (or its replacement in the new `/purchase-requests/{id}/convert-to-orcamento` endpoint) gains the same

## 5. Update existing tests for the new required fields

- [x] 5.1 `OrcamentoServiceTest`: update every direct `new Orcamento(...)` construction and `service.create(...)`/`createFromPurchaseRequestItems(...)` call to the new signatures/constructor; add a test asserting supplier find-or-create reuse (same CNPJ, no duplicate `Fornecedor`) and a test asserting a new `Fornecedor` is created for an unseen CNPJ
- [x] 5.2 `PurchaseRequestItemServiceTest` (or its renamed/split equivalent): update for header-scoped creation/listing; add a test asserting conversion rejects a selection spanning two different `PurchaseRequest` headers; add a test asserting a second conversion from the same header's remaining `PENDING` items succeeds and produces a second Orçamento
- [x] 5.3 New `FornecedorServiceTest`: find-or-create reuse vs. creation, CNPJ-prefix search (match, no-match, prefix-too-short rejection), and same-CNPJ-different-company isolation
- [x] 5.4 New `PurchaseRequestServiceTest` (if split from `PurchaseRequestItemService`): name-generation sequencing (first of the day, second of the day on the same site, independent sequence on a different site)

## 6. pantheon-web composables

- [x] 6.1 `usePurchaseRequests.ts`: replace flat item CRUD with `listPurchaseRequests(siteId, date?)`, `getPurchaseRequest(id)`, `createPurchaseRequest(siteId, items)`, `listItems(purchaseRequestId, status?)`, `convertToOrcamento(purchaseRequestId, itemIds, fornecedor)`
- [x] 6.2 New `useFornecedores.ts`: `searchByCnpjPrefix(siteId, prefix)` returning `FornecedorSuggestion[]`
- [x] 6.3 `useOrcamentos.ts`: `Orcamento`/`OrcamentoDetail` types gain the supplier snapshot fields, `sourcePurchaseRequestId`, and the originating Pedido de Compra's name; `createOrcamento` and the conversion call both take a `FornecedorInput`; `listOrcamentos(siteId, { date?, purchaseRequestId? })`

## 7. pantheon-web components and views

- [x] 7.1 New `FornecedorPicker.vue` (or inline composable logic in a shared form section): CNPJ input that queries `useFornecedores().searchByCnpjPrefix` once 5+ digits are entered, shows a suggestion dropdown, fills name/address/contact on selection, and lets the user type the remaining fields manually when there's no match — used by both Orçamento-creation entry points
- [x] 7.2 `PurchaseRequestPanel.vue` reworked around `PurchaseRequest` documents: list of Pedidos de Compra (with a date filter), a creation form that adds one or more items at once under a new header, and a detail/expansion per header showing its pending/converted items with selection + "Criar orçamento" (opening the `FornecedorPicker` step) that stays open afterward for any remaining pending items
- [x] 7.3 `OrcamentoListPanel.vue`: date filter and Pedido-de-Compra filter on the list; "Novo orçamento" now opens the supplier form (`FornecedorPicker`) before creating the blank Orçamento
- [x] 7.4 `OrcamentoDetailView.vue` redesign: a supplier card (CNPJ/name/address/contact), a link to the originating Pedido de Compra when `sourcePurchaseRequestId` is set, clearer status presentation, a more legible approval timeline (group by cycle), a line-items table with a computed total row when unit prices are present, and the post-conclusion materials section visually separated (e.g. its own bordered/tinted block) from the rest
- [x] 7.5 `router/index.ts`: add `/purchase-requests/:id` route for the Pedido de Compra detail view if the rework introduces a dedicated view rather than an in-panel expansion (skip if the panel handles it inline)

## 8. i18n

- [x] 8.1 `pt-BR.json`: rewrite `purchaseRequests.*` for the header/document model (list, date filter, creation form, per-header item selection); add `fornecedor.*` (CNPJ label, autocomplete hints, form fields); extend `orcamento.*` with supplier fields, the Pedido de Compra link, and list filters

## 9. Tests & verification

- [x] 9.1 `pantheon-service`: `mvn -q -o compile`, `mvn -q -o test-compile`, and `mvn -q -o test` all pass (real Postgres + Flyway V40-43 applied cleanly)
- [x] 9.2 `pantheon-web`: `vue-tsc -b --force` and `vite build` both pass clean
- [ ] 9.3 Manual QA in a live browser: create a Pedido de Compra with two items, convert one item into an Orçamento with a brand-new CNPJ (verify it registers a Fornecedor), convert the remaining item from the same Pedido de Compra into a second Orçamento reusing the same CNPJ (verify no duplicate Fornecedor and autocomplete surfaces it from the 5th digit), filter the Orçamento list by that Pedido de Compra and by date, open the redesigned Orçamento detail view and confirm the supplier card, origin link, and totals render correctly
- [x] 9.4 Update this file's checkboxes to reflect actual completion as work proceeds
- [ ] 9.5 Run `openspec archive` once implemented and verified, updating `openspec/specs/` accordingly
