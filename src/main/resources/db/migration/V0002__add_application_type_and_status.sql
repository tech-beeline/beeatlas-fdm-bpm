CREATE SEQUENCE processes.seq_application_id START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE processes.seq_comments_id START WITH 1 INCREMENT BY 1;

CREATE TABLE processes.application_type_enum
(
    id          INTEGER PRIMARY KEY,
    alias       TEXT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    target_call TEXT,
    entity_type TEXT
);

CREATE TABLE processes.application_type_status
(
    id                    INTEGER PRIMARY KEY,
    alias                 TEXT    NOT NULL,
    name                  TEXT    NOT NULL,
    type_id               INTEGER NOT NULL,
    serial_number         INTEGER NOT NULL,
    message               TEXT    NOT NULL,
    is_end_status         BOOLEAN NOT NULL DEFAULT false,
    is_author_responsible BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (type_id) REFERENCES processes.application_type_enum (id)
);

CREATE TABLE processes.application
(
    id             INTEGER DEFAULT nextval('processes.seq_application_id') PRIMARY KEY,
    type_id        INTEGER NOT NULL,
    status_id      INTEGER NOT NULL,
    author_id      INTEGER NOT NULL,
    process_id     TEXT    NOT NULL,
    business_key   TEXT    NOT NULL,
    executer_id    INTEGER,
    name           TEXT,
    responsible_id INTEGER,
    create_date    TIMESTAMP WITHOUT TIME ZONE,
    update_date    TIMESTAMP WITHOUT TIME ZONE,
    entity_id      INTEGER NOT NULL,
    FOREIGN KEY (type_id) REFERENCES processes.application_type_enum (id),
    FOREIGN KEY (status_id) REFERENCES processes.application_type_status (id)
);

CREATE TABLE processes.comments
(
    id             INTEGER DEFAULT nextval('processes.seq_comments_id') PRIMARY KEY,
    comment        TEXT    NOT NULL,
    created_date   TIMESTAMP WITHOUT TIME ZONE,
    full_name      TEXT    NOT NULL,
    application_id INTEGER NOT NULL,
    FOREIGN KEY (application_id) REFERENCES processes.application (id)
);

CREATE TABLE processes.executor_roles
(
    id      INTEGER PRIMARY KEY,
    type_id INTEGER NOT NULL,
    role    TEXT    NOT NULL,
    FOREIGN KEY (type_id) REFERENCES processes.application_type_enum (id)
);

INSERT INTO processes.application_type_enum (id, alias, name, description, target_call, entity_type)
VALUES (1, 'create_business_capability', 'Создание бизнес-возможности',
        'Заявка на создание бизнес-возможности, согласуется корпоративным архитектором, срок рассмотрения 5 дней',
        'https://capability-backend-devefdmmart.apps.yd-m6-kt22.vimpelcom.ru/api/v1/business-capability/public/{id}',
        'BUSINESS_CAPABILITY'),
       (2, 'update_business_capability', 'Редактирование бизнес-возможности',
        'Заявка на редактирование бизнес-возможности, согласуется корпоративным архитектором, срок рассмотрения 5 дней',
        'https://capability-backend-devefdmmart.apps.yd-m6-kt22.vimpelcom.ru/api/v1/business-capability/public/{id}',
        'BUSINESS_CAPABILITY');

INSERT INTO processes.executor_roles (id, type_id, role)
VALUES (1, 1, 'ADMINISTRATOR');

INSERT INTO processes.executor_roles (id, type_id, role)
VALUES (2, 2, 'ADMINISTRATOR');

INSERT INTO processes.application_type_status (id, alias, name, type_id, serial_number, message, is_end_status,
                                               is_author_responsible)
VALUES (1, 'wtxctr', 'Ожидает исполнителя', 1, 1, 'order_create', false, false),
       (2, 'rw', 'На рассмотрении', 1, 2, 'change_responsible', false, false),
       (3, 'rfctr', 'На доработке', 1, 2, 'change_responsible', false, true),
       (4, 'dn', 'Одобрено', 1, 3, 'done', true, true),
       (5, 'cncl', 'Отклонена', 1, 3, 'cancel', true, true);

INSERT INTO processes.application_type_status (id, alias, name, type_id, serial_number, message, is_end_status,
                                               is_author_responsible)
VALUES (6, 'wtxctr', 'Ожидает исполнителя', 2, 1, 'order_create', false, false),
       (7, 'rw', 'На рассмотрении', 2, 2, 'change_responsible', false, false),
       (8, 'rfctr', 'На доработке', 2, 2, 'change_responsible', false, true),
       (9, 'dn', 'Одобрено', 2, 3, 'done', true, true),
       (10, 'cncl', 'Отклонена', 2, 3, 'cancel', true, true);
