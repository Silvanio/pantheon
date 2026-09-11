-- Dev-only manual QA rows from the prior change, cleared so the fornecedor NOT NULL columns
-- added below need no backfill. No business value.
DELETE FROM material_delivery_photo;
DELETE FROM material;
DELETE FROM orcamento_approval;
DELETE FROM orcamento_line_item;
DELETE FROM purchase_request_item;
DELETE FROM orcamento;
