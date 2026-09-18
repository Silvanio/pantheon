## Context

`Fornecedor` already has an established "optional field, snapshotted onto Orcamento at creation" pattern for address/contact-name/contact-phone: `FornecedorService.findOrCreate` persists whatever was submitted (only for a genuinely new CNPJ — an existing one ignores the submission entirely), and `OrcamentoService.createDraft` copies the resolved `Fornecedor`'s fields onto the new `Orcamento` row. Payment method is a straightforward extension of that same pattern, with one added wrinkle: a conditional required field (Pix key, only when the method is Pix).

## Goals / Non-Goals

**Goals:**
- Let a payment method be recorded when a supplier is registered (creating a new `Fornecedor`), with a Pix key required exactly when the method is Pix.
- Carry that data onto the `Orcamento` snapshot and surface it in the detail view, matching how address/contact already work end-to-end.
- Reuse the existing shared `FornecedorPicker.vue` so every Orçamento-creation entry point gets the new field for free, per `supplier-registry`'s existing "every Orçamento-creation flow" requirement.

**Non-Goals:**
- Editing a `Fornecedor`'s payment method after creation — `Fornecedor` is immutable once created (existing rule), and this change doesn't touch that.
- Validating a Pix key's format (CPF/CNPJ/email/phone/random-key shape). It is free text, like every other contact field on `Fornecedor`.
- Any change to the find-or-create dedup rule itself — a reused (existing-CNPJ) `Fornecedor` still ignores whatever payment method/Pix key was submitted alongside it, exactly like it already ignores name/address/contact today.

## Decisions

**1. `FornecedorPaymentMethod` enum: `CARTAO`, `BOLETO`, `PIX`, `DINHEIRO`.**
Both fields are nullable/optional at the entity and DB level (a supplier registered before this change, or one where the field is simply left blank, has `paymentMethod = null`). This matches every other optional `Fornecedor` field (address, contact) rather than forcing a backfill or a default value with a different meaning than "not recorded."

**2. Pix-key requirement is validated in `FornecedorService.findOrCreate`, not with bean validation on the DTO.**
It's a cross-field rule ("required only when `paymentMethod == PIX`"), which `jakarta.validation` annotations can't express cleanly at the record-field level without a custom validator class — and the rule only actually matters at the one call site that creates a *new* `Fornecedor` (an existing one ignores the submission regardless). A plain `if` in the service, throwing a new `PixKeyRequiredException` (mapped to HTTP 400, matching the style of every other simple domain-rule exception in `ConstructionExceptionHandler`), is simpler and more precisely scoped than a Bean Validation constraint that would also have to somehow not fire on the reuse path.

**3. Non-Pix method with a Pix key present: silently drop the key rather than reject.**
Symmetrical treatment would be to also reject "Pix key present but method isn't Pix," but that adds friction for zero benefit — e.g. a user who typed a Pix key then changed their mind about the method shouldn't be blocked by a leftover value the UI itself should have cleared. `FornecedorService.findOrCreate` nulls out the key whenever `paymentMethod != PIX` before persisting.

**4. `Orcamento` gets two more `fornecedor_*`-prefixed snapshot columns, following the exact naming convention already established** (`fornecedor_forma_pagamento`, `fornecedor_pix_key`), populated in the same `OrcamentoService.createDraft` call that already copies every other `Fornecedor` field onto the new `Orcamento`.

## Risks / Trade-offs

- **[Risk] Enum values are Portuguese business terms (`CARTAO`/`BOLETO`/`PIX`/`DINHEIRO`) rather than English, breaking from most other enums in this codebase (`PurchaseRequestStatus`, `OrcamentoStatus`, etc., which are also actually Portuguese status words — `INICIADO`/`ORCADO`/`DRAFT` is the exception, not the rule).** → Not a real risk: matches the existing convention of using the Portuguese domain term as the enum constant (see `PurchaseRequestStatus.CONFERIDO`, `ConstructionFunction.ENGINEER` is the outlier, not `PurchaseRequestStatus`).
- **[Trade-off] No migration backfill for existing `Fornecedor`/`Orcamento` rows** — they simply have `payment_method = NULL`, rendered as "not informed" in the UI. Consistent with how every other nullable snapshot field already behaves for pre-existing rows.

## Migration Plan

Additive: two nullable columns on `fornecedor`, two nullable columns on `orcamento` (one migration file). No backfill, no behavior change for existing data. Rollback is a normal revert.

## Open Questions

None.
