ALTER TABLE processes.status_process
    ADD COLUMN IF NOT EXISTS sequence INTEGER;

UPDATE processes.status_process
SET sequence = 1
WHERE sequence IS NULL;

