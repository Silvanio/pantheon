## Why

The `Fornecedor` (supplier) registry captures CNPJ, name, address, and contact, but no payment method — sites need to record how each supplier is to be paid (Cartão, Boleto, Pix, or Dinheiro) so that information travels with the Orçamento, same as address/contact already do.

## What Changes

- `Fornecedor` gains an optional `paymentMethod` (`CARTAO` | `BOLETO` | `PIX` | `DINHEIRO`) and, only when `paymentMethod == PIX`, a required `pixKey`. Submitting a `PIX` payment method with no key is rejected; a non-`PIX` method with a key present simply ignores the key (it is not persisted).
- `Orcamento` snapshots the resolved supplier's `paymentMethod`/`pixKey` at creation time, exactly like it already snapshots address/contact — this is the existing "find-or-create then snapshot" pattern, extended by two fields, not a new mechanism.
- `pantheon-web`'s shared `FornecedorPicker.vue` (used by every Orçamento-creation flow — blank creation and creation from selected Pedido de Compra items) gets a payment-method selector; selecting "Pix" reveals a Pix-key input, any other selection hides it and clears it.
- The Orçamento detail view shows the recorded payment method (and Pix key, when applicable) alongside the rest of the supplier snapshot.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `supplier-registry`: extends the `Fornecedor` record and its creation/reuse rule with payment method and conditional Pix key; extends the Orçamento-creation UI requirement to include the new fields.
- `orcamento-management`: extends the Orçamento-creation requirement's supplier-snapshot wording, and its listing/detail requirement, to include the new fields.

## Impact

- `pantheon-service`: new `FornecedorPaymentMethod` enum, `Fornecedor`/`Orcamento` entity columns (migration), `FornecedorRequest`/`FornecedorResponse`/`OrcamentoResponse` DTOs, a validation rejection in `FornecedorService.findOrCreate` (new Fornecedor + `PIX` + blank key), `OrcamentoService.createDraft`'s snapshot call.
- `pantheon-web`: `useFornecedores.ts`, `useOrcamentos.ts`, `FornecedorPicker.vue`, `OrcamentoDetailView.vue`, `pt-BR.json`.
