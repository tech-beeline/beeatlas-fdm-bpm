INSERT INTO processes.status_process (id,
                                      type_process_id,
                                      name,
                                      alias,
                                      is_done,
                                      is_error)
SELECT COALESCE(MAX(id), 0) + 1,
       1,
       'Ошибка создания',
       'errcrt',
       false,
       true
FROM processes.status_process;
