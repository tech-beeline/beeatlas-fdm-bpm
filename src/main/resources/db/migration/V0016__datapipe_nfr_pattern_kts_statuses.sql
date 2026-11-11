-- Process_08wprkp: статусы НФТ и паттернов, завершение процесса переносится на паспорт КТС.
-- Тип DatapipeBranch не затрагивается.

UPDATE processes.status_process
SET is_done = false
WHERE type_process_id = 1
  AND alias = 'vpcrt';

UPDATE processes.status_process
SET sequence = 17,
    is_done  = true
WHERE type_process_id = 1
  AND alias = 'ktscrt';

UPDATE processes.status_process
SET sequence = 17
WHERE type_process_id = 1
  AND alias = 'ktserr';

WITH src(alias, name, is_done, is_error, sequence) AS (
    VALUES ('nfrcrt',
            'НФТ назначены на продукт',
            false, false, 13),
           ('nfrerr',
            'Ошибка назначения НФТ на продукт',
            false, true, 13),
           ('ptrncrt',
            'Паттерны продукта рассчитаны',
            false, false, 14),
           ('ptrnerr',
            'Ошибка расчёта паттернов продукта',
            false, true, 14)
),
     numbered AS (
         SELECT *, row_number() OVER (ORDER BY sequence, alias) AS rn
         FROM src
     )
INSERT
INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT (SELECT COALESCE(MAX(id), 0) FROM processes.status_process) + rn,
       1,
       name,
       alias,
       is_done,
       is_error,
       sequence
FROM numbered;
