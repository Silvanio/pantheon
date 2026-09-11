## ADDED Requirements

### Requirement: Supplier find-or-create by CNPJ
`pantheon-service` SHALL maintain a `Fornecedor` (supplier) record per `Company`, uniquely identified within that company by CNPJ, recording a required name, an optional address, and an optional contact name and phone. When an Orçamento is created with supplier data whose CNPJ already matches a `Fornecedor` of that Orçamento's site's company, `pantheon-service` SHALL reuse that existing `Fornecedor` (ignoring any differences in the submitted name/address/contact); otherwise it SHALL create a new `Fornecedor` from the submitted data. A `Fornecedor` is never edited once created.

#### Scenario: New CNPJ registers a supplier
- **WHEN** an Orçamento is created with a CNPJ that has no matching `Fornecedor` on the site's company
- **THEN** `pantheon-service` creates a new `Fornecedor` for that company from the submitted name/address/contact

#### Scenario: Known CNPJ reuses the existing supplier
- **WHEN** an Orçamento is created with a CNPJ that already matches a `Fornecedor` on the site's company
- **THEN** `pantheon-service` reuses that `Fornecedor` and does not create a duplicate, regardless of any differences in the newly submitted name/address/contact

#### Scenario: Same CNPJ registered under different companies stays separate
- **WHEN** two different companies each create an Orçamento using the same CNPJ
- **THEN** `pantheon-service` maintains two distinct `Fornecedor` records, one per company

### Requirement: Supplier reuse across construction sites
`pantheon-service` SHALL scope every `Fornecedor` to the owning `Company`, not to an individual construction site, so that a supplier registered while creating an Orçamento on one site is available for reuse on every other site of that same company.

#### Scenario: Supplier registered on one site appears on another
- **WHEN** a `Fornecedor` is registered while creating an Orçamento on one construction site of a company
- **THEN** a CNPJ-prefix search on another construction site of the same company returns that `Fornecedor`

### Requirement: Supplier search by CNPJ prefix
`pantheon-service` SHALL allow any member with access to a construction site to search that site's company's `Fornecedor` records by a CNPJ prefix of at least 5 digits, returning matching suppliers' CNPJ, name, address, and contact for autocomplete. This search does not require `ORCAMENTO_MANAGE` or `PURCHASE_REQUEST` access — read access to the site is sufficient.

#### Scenario: Prefix search returns matches
- **WHEN** a construction site member searches suppliers by a 5-digit CNPJ prefix that matches one registered `Fornecedor` of the site's company
- **THEN** `pantheon-service` returns that `Fornecedor`'s CNPJ, name, address, and contact

#### Scenario: Prefix search with no matches
- **WHEN** a construction site member searches suppliers by a CNPJ prefix that matches no registered `Fornecedor` of the site's company
- **THEN** `pantheon-service` returns an empty result, allowing the member to proceed with manual registration

### Requirement: Supplier autocomplete in the Orçamento creation UI
`pantheon-web` SHALL, in every Orçamento-creation flow (blank creation and creation from selected Pedido de Compra items), present a CNPJ field that queries the supplier-search endpoint once at least 5 digits have been entered, show matching suggestions, and fill in name/address/contact fields when a suggestion is selected. When no suggestion matches, the user SHALL be able to complete the remaining fields manually.

#### Scenario: User selects a suggested supplier
- **WHEN** a user typing a CNPJ in the Orçamento creation form selects a suggested supplier
- **THEN** `pantheon-web` fills the name, address, and contact fields from that suggestion

#### Scenario: User registers a new supplier inline
- **WHEN** a user types a CNPJ with no matching suggestions and fills in the remaining supplier fields manually
- **THEN** `pantheon-web` submits the supplier data with the Orçamento creation request, which registers the new `Fornecedor`
