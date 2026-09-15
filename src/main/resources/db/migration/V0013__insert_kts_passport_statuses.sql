INSERT INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT COALESCE(MAX(id), 0) + 1,
       1,
       'Паспорт КТС сгенерирован в SmartKTS',
       'ktscrt',
       false,
       false,
       13
FROM processes.status_process;

INSERT INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT COALESCE(MAX(id), 0) + 1,
       1,
       'При генерации паспорта КТС в SmartKTS произошла ошибка',
       'ktserr',
       false,
       true,
       13
FROM processes.status_process;
