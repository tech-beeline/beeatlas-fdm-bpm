UPDATE processes.status_process
SET is_done = false
WHERE alias = 'stcrt';

INSERT INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error)
VALUES (21, 1, 'Данные архитектуры опубликованы и переданы в платформу наблюдаемости для перестроения dashboard', 'vpcrt', true, false);

INSERT INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error)
VALUES (22, 1, 'При передачи данных в платформу наблюдаемости произошла ошибка', 'vperr', false, true);