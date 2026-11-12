-- Тип перерасчёта fitness function и статусы родителя Process_recalc_ff.
-- Частный Process_recalc_ff_product отдельным типом не регистрируется.

INSERT INTO processes.type_process (id, name, description, alias)
VALUES (4,
        'Перерасчёт fitness function',
        'Расчёт одной fitness function по продуктам со structurizr',
        'RecalcFf');

WITH src(alias, name, is_done, is_error, sequence) AS (
    VALUES ('crt',
            'Процесс перерасчёта fitness function зарегистрирован',
            false, false, 1),
           ('errcrt',
            'Ошибка регистрации процесса перерасчёта fitness function',
            false, true, 1),
           ('ffsub',
            'Запущены процессы расчёта fitness function по продуктам',
            false, false, 2),
           ('fferr',
            'Ошибка расчёта fitness function',
            false, true, 2),
           ('ffdn',
            'Расчёт fitness function по всем продуктам завершён',
            true, false, 3)
),
     numbered AS (
         SELECT *, row_number() OVER (ORDER BY sequence, alias) AS rn
         FROM src
     )
INSERT
INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT (SELECT COALESCE(MAX(id), 0) FROM processes.status_process) + rn,
       4,
       name,
       alias,
       is_done,
       is_error,
       sequence
FROM numbered;
