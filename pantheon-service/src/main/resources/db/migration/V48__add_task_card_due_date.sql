-- Optional expected-completion date for a task card, day-granularity, no time component.
ALTER TABLE task_card ADD COLUMN due_date DATE;
