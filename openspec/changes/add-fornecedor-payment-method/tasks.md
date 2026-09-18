## 1. Backend — data model

- [x] 1.1 Migration `V56__add_fornecedor_payment_method.sql`: add nullable `payment_method VARCHAR(20)` and `pix_key VARCHAR(255)` to `fornecedor`; add the same two nullable columns to `orcamento` (as `fornecedor_forma_pagamento`/`fornecedor_pix_key`, matching its existing `fornecedor_*` snapshot column naming).
- [x] 1.2 New enum `FornecedorPaymentMethod { CARTAO, BOLETO, PIX, DINHEIRO }` in the entity package.
- [x] 1.3 `Fornecedor` entity: add `paymentMethod`/`pixKey` fields + getters, appended to the end of the constructor's parameter list (minimizes churn at existing call sites — append `null, null` rather than reordering).
- [x] 1.4 `Orcamento` entity: add `fornecedorFormaPagamento`/`fornecedorPixKey` fields + getters, same append-at-the-end approach on the constructor.
- [x] 1.5 New `PixKeyRequiredException`, registered in `ConstructionExceptionHandler` mapped to HTTP 400.

## 2. Backend — DTOs and service logic

- [x] 2.1 `FornecedorRequest`: add `FornecedorPaymentMethod paymentMethod` and `String pixKey` fields (no bean-validation annotations on them — the conditional rule is enforced in the service, per design.md decision 2).
- [x] 2.2 `FornecedorResponse`: add `paymentMethod`/`pixKey`, populated `from(Fornecedor)`.
- [x] 2.3 `OrcamentoResponse`: add `fornecedorFormaPagamento`/`fornecedorPixKey`, populated `from(Orcamento, ...)`.
- [x] 2.4 `FornecedorService.findOrCreate`: when creating a *new* `Fornecedor` (the `orElseGet` branch), if `request.paymentMethod() == PIX` and `request.pixKey()` is blank/null, throw `PixKeyRequiredException`; otherwise persist `paymentMethod` as submitted and `pixKey` only when `paymentMethod == PIX` (null otherwise). The existing-CNPJ reuse branch is unchanged (already ignores every non-CNPJ field).
- [x] 2.5 `OrcamentoService.createDraft`: pass `fornecedor.getPaymentMethod()`/`fornecedor.getPixKey()` into the new `Orcamento(...)` call.

## 3. Backend — tests

- [x] 3.1 Update `FornecedorServiceTest`'s and `OrcamentoServiceTest`'s/`PurchaseRequestItemServiceTest`'s existing `new Fornecedor(...)`/`new FornecedorRequest(...)` call sites for the new trailing constructor params.
- [x] 3.2 `FornecedorServiceTest`: new cases — creating with `PIX` + blank key throws `PixKeyRequiredException`; creating with `PIX` + a key persists both; creating with a non-`PIX` method + a key persists the method and discards the key; reuse path ignores payment method/key differences (extend the existing reuse test).
- [x] 3.3 `OrcamentoServiceTest`: a create-draft case asserting the resolved `Fornecedor`'s payment method/Pix key land on the new `Orcamento`.
- [x] 3.4 `./mvnw -pl pantheon-service test` green.

## 4. Frontend

- [x] 4.1 `useFornecedores.ts`: add `paymentMethod`/`pixKey` to `FornecedorSuggestion` and `FornecedorInput`; export a `FornecedorPaymentMethod` union type (`'CARTAO' | 'BOLETO' | 'PIX' | 'DINHEIRO'`).
- [x] 4.2 `useOrcamentos.ts`: add `fornecedorFormaPagamento`/`fornecedorPixKey` to the `Orcamento` interface.
- [x] 4.3 `FornecedorPicker.vue`: add a payment-method select (Cartão/Boleto/Pix/Dinheiro, plus a blank/unselected option) and a Pix-key text input shown only when the method is `PIX`; clear the Pix-key value whenever the method changes away from `PIX`; `selectSuggestion` fills both fields from the chosen suggestion; `onContinue` includes both in the emitted `FornecedorInput` (`pixKey: null` unless the method is `PIX`).
- [x] 4.4 `OrcamentoDetailView.vue`: in the supplier section, show the payment method's localized label, and the Pix key only when the method is `PIX`.
- [x] 4.5 `pt-BR.json`: add `fornecedor.paymentMethodLabel`, `fornecedor.paymentMethod.{CARTAO,BOLETO,PIX,DINHEIRO}`, `fornecedor.pixKeyLabel`, and an `orcamento.pixKeyLabel` (or reuse `fornecedor.pixKeyLabel`) for the detail view.
- [x] 4.6 `npm run build` green in `pantheon-web`.

## 5. Verification

- [x] 5.1 Run `openspec validate --strict` against the change before archiving.
