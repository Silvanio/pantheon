-- "Outro equipamento" (not registered) mirrors daily_report_workforce_entry's existing
-- nullable membership_id + role_description fallback pattern: equipment_id becomes optional and
-- a new custom_name column holds the free-text name when the equipment isn't registered. Existing
-- rows all have equipment_id set, so the widened column and the new constraint both hold trivially
-- for current data (additive/widening change — see the change's design.md Decision 2).
ALTER TABLE daily_report_equipment_usage ALTER COLUMN equipment_id DROP NOT NULL;
ALTER TABLE daily_report_equipment_usage ADD COLUMN custom_name VARCHAR(255);

ALTER TABLE daily_report_equipment_usage
    ADD CONSTRAINT daily_report_equipment_usage_ref_or_custom CHECK (
        equipment_id IS NOT NULL OR custom_name IS NOT NULL
    );
