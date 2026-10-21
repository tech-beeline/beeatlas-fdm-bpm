ALTER TABLE processes.camunda_process_status
    ADD COLUMN message TEXT NULL;

COMMENT ON COLUMN processes.camunda_process_status.message IS 'Причина ошибки шага для пользователя, NULL для успешных статусов';
