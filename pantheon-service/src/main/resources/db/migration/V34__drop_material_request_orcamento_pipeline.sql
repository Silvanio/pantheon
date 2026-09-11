-- Restructures the materials/orçamento area: the old catalog'd Material -> MaterialRequest ->
-- single-approval Orcamento -> ReceiptVerification pipeline is replaced by a free-text
-- PurchaseRequestItem -> Orcamento (multi-level approval) -> Material (delivery-tracking)
-- pipeline. See openspec/changes/restructure-materials-orcamento-flow/design.md.

-- Preserve existing daily-report material-received data as free text before the catalog goes away.
ALTER TABLE daily_report_material_received ADD COLUMN material_name VARCHAR(255);
ALTER TABLE daily_report_material_received ADD COLUMN unit VARCHAR(50);

UPDATE daily_report_material_received drm
SET material_name = m.name, unit = m.unit
FROM material m
WHERE m.id = drm.material_id;

ALTER TABLE daily_report_material_received ALTER COLUMN material_name SET NOT NULL;
ALTER TABLE daily_report_material_received DROP COLUMN material_id;

DROP TABLE receipt_verification_photo;
DROP TABLE receipt_verification;
DROP TABLE orcamento_attachment;
DROP TABLE orcamento_line_item;
DROP TABLE orcamento;
DROP TABLE material_request_item;
DROP TABLE material_request;
DROP TABLE material;

DELETE FROM site_permission_override
WHERE capability IN ('EQUIPMENT_MATERIAL', 'MATERIAL_REQUEST', 'MATERIAL_APPROVAL');
