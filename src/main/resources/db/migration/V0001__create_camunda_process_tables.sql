
CREATE SEQUENCE processes.seq_camunda_process_status_id
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE processes.seq_camunda_process_id
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE processes.seq_context_id
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE processes.type_process
(
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT NOT NULL,
    alias       TEXT NOT NULL
);

CREATE TABLE processes.status_process
(
    id              INTEGER PRIMARY KEY,
    type_process_id INTEGER NOT NULL REFERENCES processes.type_process (id),
    name            TEXT    NOT NULL,
    alias           TEXT    NOT NULL UNIQUE,
    is_done         BOOLEAN,
    is_error        BOOLEAN
);

CREATE TABLE processes.camunda_process
(
    id              INTEGER PRIMARY KEY DEFAULT nextval('processes.seq_camunda_process_id'),
    type_process_id INTEGER NOT NULL REFERENCES processes.type_process (id),
    proc_id         TEXT    NOT NULL,
    business_key    TEXT    NOT NULL
);

CREATE TABLE processes.camunda_process_status
(
    id                 INTEGER PRIMARY KEY DEFAULT nextval('processes.seq_camunda_process_status_id'),
    status_process_id  INTEGER NOT NULL REFERENCES processes.status_process (id),
    camunda_process_id INTEGER NOT NULL REFERENCES processes.camunda_process (id),
    created_date       TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE processes.context
(
    id                 INTEGER PRIMARY KEY DEFAULT nextval('processes.seq_context_id'),
    camunda_process_id INTEGER NOT NULL REFERENCES processes.camunda_process (id),
    name               TEXT    NOT NULL,
    value              TEXT    NOT NULL
);

INSERT INTO processes.type_process (id, name, description, alias)
VALUES (1, 'Structurizr DataPipeLine', 'Пайплайн публикации архитектуры в Structurizr', 'Datapipe');

INSERT INTO processes.status_process (id, type_process_id, name, alias, is_done, is_error)
VALUES (1, 1, 'Создан', 'crt', false, false),
       (2, 1, 'Выгружен локальный граф', 'lclgrph', false, false),
       (3, 1, 'Ошибка при построении локального графа', 'errlclgrph', false, true),
       (4, 1, 'Fitness functions рассчитаны', 'ffdn', false, false),
       (5, 1, 'При проверке Fitness functions выявлены ошибки архитектуры', 'fferr', false, true),
       (6, 1, 'При проверке Fitness functions произошла программная ошибка', 'fferrcd', false, true),
       (7, 1, 'Выгружен глобальный граф', 'glblgrph', false, false),
       (8, 1, 'Ошибка при построении глобального графа', 'errglblgrph', false, true),
       (9, 1, 'Глобальные Fitness functions рассчитаны', 'glbffdn', false, false),
       (10, 1, 'При проверке глобальных Fitness functions выявлены ошибки архитектуры', 'glblfferr', false, true),
       (11, 1, 'При проверке глобальных Fitness functions произошла программная ошибка', 'glblfferrcd', false, true),
       (12, 1, 'Данные архитектуры выгружены в Beeatlas', 'btls', false, false),
       (13, 1, 'При выгрузке данных в BeeAtlas произошла ошибка', 'btlserr', false, true),
       (14, 1, 'Данные архитектуры выгружены в structurizr', 'strct', true, false),
       (15, 1, 'При выгрузке данных в structurizr произошла ошибка', 'strcterr', false, true);
