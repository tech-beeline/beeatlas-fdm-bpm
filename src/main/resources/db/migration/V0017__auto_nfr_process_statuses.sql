-- Тип автоназначения НФТ и статусы родителя Process_1t2rjem.
-- Process_053fkyf отдельным типом не регистрируется.
-- Номер V0017: V0016 уже занят статусами DataPipe.

INSERT INTO processes.type_process (id, name, description, alias)
VALUES (3,
        'Автоназначение НФТ и расчёт паттернов',
        'Назначение изменённого НФТ на продукты, прошедшие правило, и расчёт связанных паттернов',
        'AutoNfr');

WITH src(alias, name, is_done, is_error, sequence) AS (
    VALUES ('crt',
            'Процесс автоназначения НФТ зарегистрирован',
            false, false, 1),
           ('errcrt',
            'Ошибка регистрации процесса автоназначения НФТ',
            false, true, 1),
           ('nfrsub',
            'Запущены процессы назначения НФТ и расчёта паттернов по продуктам',
            false, false, 3),
           ('nfrerr',
            'Ошибка назначения НФТ и расчёта паттернов',
            false, true, 3),
           ('nfrdn',
            'Назначение НФТ и расчёт паттернов по всем продуктам завершены',
            true, false, 5)
),
     numbered AS (
         SELECT *, row_number() OVER (ORDER BY sequence, alias) AS rn
         FROM src
     )
INSERT
INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT (SELECT COALESCE(MAX(id), 0) FROM processes.status_process) + rn,
       3,
       name,
       alias,
       is_done,
       is_error,
       sequence
FROM numbered;
