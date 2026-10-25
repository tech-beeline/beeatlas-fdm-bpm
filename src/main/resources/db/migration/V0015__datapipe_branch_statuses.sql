-- Тип DataPipe ветки архитектуры и статусы, которые пишет Process_datapipeline_branch.
-- Алиас статуса был уникален на всю таблицу. Тот же алиас нужен второму типу,
-- потому что существующие делегаты ищут статус по алиасу и type_process_id процесса.

DO $$
DECLARE
    alias_unique text;
BEGIN
    SELECT c.conname
    INTO alias_unique
    FROM pg_constraint c
             JOIN pg_class t ON t.oid = c.conrelid
             JOIN pg_namespace n ON n.oid = t.relnamespace
             JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
    WHERE n.nspname = 'processes'
      AND t.relname = 'status_process'
      AND c.contype = 'u'
    GROUP BY c.conname
    HAVING array_agg(a.attname::text ORDER BY a.attnum) = ARRAY ['alias']::text[];

    IF alias_unique IS NOT NULL THEN
        EXECUTE format('ALTER TABLE processes.status_process DROP CONSTRAINT %I', alias_unique);
    END IF;
END $$;

ALTER TABLE processes.status_process
    DROP CONSTRAINT IF EXISTS status_process_alias_type_process_uk;

ALTER TABLE processes.status_process
    ADD CONSTRAINT status_process_alias_type_process_uk UNIQUE (alias, type_process_id);

INSERT INTO processes.type_process (id, name, description, alias)
VALUES (2,
        'DataPipe ветки архитектуры',
        'Сохранение архитектуры продукта в ветке через staging',
        'DatapipeBranch');

WITH src AS (
    SELECT alias,
           name,
           is_done,
           is_error,
           CASE alias
               WHEN 'crt' THEN 1
               WHEN 'vldterr' THEN 1
               WHEN 'cmdberr' THEN 1
               WHEN 'errcrt' THEN 1
               WHEN 'glbltskcrt' THEN 7
               WHEN 'glblgrph' THEN 9
               WHEN 'errglblgrph' THEN 9
               WHEN 'ffdn' THEN 11
               WHEN 'fferrcd' THEN 11
               WHEN 'ktscrt' THEN 13
               WHEN 'ktserr' THEN 13
               WHEN 'vpcrt' THEN 15
               WHEN 'vperr' THEN 15
               END AS sequence
    FROM processes.status_process
    WHERE type_process_id = 1
      AND alias IN (
                    'crt', 'vldterr', 'cmdberr', 'errcrt',
                    'glbltskcrt', 'glblgrph', 'errglblgrph',
                    'ffdn', 'fferrcd',
                    'ktscrt', 'ktserr',
                    'vpcrt', 'vperr'
        )
),
     numbered AS (
         SELECT *, row_number() OVER (ORDER BY sequence, alias) AS rn
         FROM src
     )
INSERT
INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT (SELECT COALESCE(MAX(id), 0) FROM processes.status_process) + rn,
       2,
       name,
       alias,
       is_done,
       is_error,
       sequence
FROM numbered;

WITH src(alias, name, is_done, is_error, sequence) AS (
    VALUES ('stgarchcrt',
            'Запущен пайп выгрузки архитектуры, технических возможностей и технологий',
            false, false, 3),
           ('stgarchdn',
            'Архитектура, технические возможности и технологии выгружены в ветку',
            false, false, 3),
           ('stgarcherr',
            'Ошибка выгрузки архитектуры, технических возможностей и технологий',
            false, true, 3),
           ('stgseqcrt',
            'Запущен пайп выгрузки dynamic view',
            false, false, 5),
           ('stgseqdn',
            'Dynamic view опубликованы в ветку',
            false, false, 5),
           ('stgseqwop',
            'Публикация dynamic view отклонена',
            false, false, 5),
           ('stgseqerr',
            'Ошибка выгрузки dynamic view',
            false, true, 5)
),
     numbered AS (
         SELECT *, row_number() OVER (ORDER BY sequence, alias) AS rn
         FROM src
     )
INSERT
INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error, sequence)
SELECT (SELECT COALESCE(MAX(id), 0) FROM processes.status_process) + rn,
       2,
       name,
       alias,
       is_done,
       is_error,
       sequence
FROM numbered;
