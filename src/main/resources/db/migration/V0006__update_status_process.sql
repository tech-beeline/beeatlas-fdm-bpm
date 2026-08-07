INSERT INTO processes.status_process (
    id,
    type_process_id,
    name,
    alias,
    is_done,
    is_error
)
SELECT
    COALESCE(MAX(id), 0) + 1,
    1,
    'Создана задача на построение локального графа',
    'lcltskcrt',
    false,
    false
FROM processes.status_process;

INSERT INTO processes.status_process (
    id,
    type_process_id,
    name,
    alias,
    is_done,
    is_error
)
SELECT
    COALESCE(MAX(id), 0) + 1,
    1,
    'Ошибка ошибка при создании задачи на построение локального графа',
    'errlcltskcrt',
    false,
    true
FROM processes.status_process;

INSERT INTO processes.status_process (
    id,
    type_process_id,
    name,
    alias,
    is_done,
    is_error
)
SELECT
    COALESCE(MAX(id), 0) + 1,
    1,
    'Создана задача на построение глобального графа',
    'glbltskcrt',
    false,
    false
FROM processes.status_process;

INSERT INTO processes.status_process (
    id,
    type_process_id,
    name,
    alias,
    is_done,
    is_error
)
SELECT
    COALESCE(MAX(id), 0) + 1,
    1,
    'Ошибка при создания задачи для построения глобального графа',
    'errglbltskcrt',
    false,
    true
FROM processes.status_process; 