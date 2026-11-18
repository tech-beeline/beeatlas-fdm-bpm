ALTER TABLE processes.camunda_process
    ADD COLUMN create_date TIMESTAMP WITHOUT TIME ZONE;

UPDATE processes.camunda_process p
SET create_date = latest.created_date
FROM (
    SELECT DISTINCT ON (camunda_process_id)
           camunda_process_id,
           created_date
    FROM processes.camunda_process_status
    ORDER BY camunda_process_id, created_date DESC NULLS LAST, id DESC
) latest
WHERE p.id = latest.camunda_process_id
  AND latest.created_date IS NOT NULL;

UPDATE processes.camunda_process
SET create_date = now()
WHERE create_date IS NULL;

ALTER TABLE processes.camunda_process
    ALTER COLUMN create_date SET NOT NULL;

ALTER TABLE processes.camunda_process
    ALTER COLUMN create_date SET DEFAULT now();

CREATE INDEX idx_camunda_process_create_date_id
    ON processes.camunda_process (create_date DESC, id DESC);

CREATE INDEX idx_camunda_process_type_create_date
    ON processes.camunda_process (type_process_id, create_date DESC, id DESC);
