ALTER TABLE schedule_task DROP CONSTRAINT schedule_task_task_card_id_fkey;
ALTER TABLE schedule_task
    ADD CONSTRAINT schedule_task_task_card_id_fkey FOREIGN KEY (task_card_id) REFERENCES task_card (id) ON DELETE SET NULL;
