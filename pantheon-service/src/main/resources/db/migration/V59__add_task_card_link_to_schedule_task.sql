ALTER TABLE schedule_task ADD COLUMN task_card_id UUID REFERENCES task_card (id);
