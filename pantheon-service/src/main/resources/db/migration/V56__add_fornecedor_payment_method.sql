-- Optional payment method on a Fornecedor (Cartao/Boleto/Pix/Dinheiro), with a Pix key required
-- only for Pix. Snapshotted onto Orcamento at creation, like every other Fornecedor field.
ALTER TABLE fornecedor ADD COLUMN payment_method VARCHAR(20);
ALTER TABLE fornecedor ADD COLUMN pix_key VARCHAR(255);

ALTER TABLE orcamento ADD COLUMN fornecedor_forma_pagamento VARCHAR(20);
ALTER TABLE orcamento ADD COLUMN fornecedor_pix_key VARCHAR(255);
