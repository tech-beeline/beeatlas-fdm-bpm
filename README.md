# fdm-bpm

Сервис оркестрации бизнес-процессов платформы BeeAtlas на базе **Camunda BPM 7.20** и **Spring Boot 3.1**. Запускает BPMN-процессы (импорт/экспорт, CMDB, capability, графы, заявки и др.), хранит метаданные процессов в PostgreSQL и взаимодействует с другими сервисами экосистемы через HTTP и RabbitMQ.

## Стек

| Компонент | Версия / технология |
|-----------|---------------------|
| Java | 17 |
| Spring Boot | 3.1.1 |
| Camunda BPM | 7.20.0 |
| БД | PostgreSQL |
| Очереди | RabbitMQ |
| Миграции | Flyway (схема `processes`) |
| Метрики | Actuator, Prometheus |
| API-документация | SpringDoc OpenAPI |

## Архитектура данных

В одной БД PostgreSQL используются три datasource:

| Datasource | Назначение |
|------------|------------|
| `camunda` | Таблицы движка Camunda (`schema-update: true`) |
| `processes` | Доменные сущности процессов, Flyway-миграции в схеме `processes` |
| `git` | Чтение представления `v_fdm_gitlab_languages` (данные из GitLab) |

## Зависимости runtime

- **PostgreSQL** — обязательно
- **RabbitMQ** — обязательно (очереди и fanout для пересчётов и графов)
- **Внешние HTTP-сервисы** — auth, products, capability, CMDB, document, notification, graph, dashboard и др. (см. `integration.*` в `application.yml`)

Для локального Docker в `docker-compose.yml` заданы заглушки URL интеграций; для полноценной работы процессов нужна сеть `beeatlas-network` с реальными сервисами или переопределение переменных окружения.

## Быстрый старт (Docker)

Требования: Docker и Docker Compose v2.

```bash
docker compose up --build -d
```

Проверка готовности:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

### Доступные адреса

| Сервис | URL | Учётные данные по умолчанию |
|--------|-----|-----------------------------|
| REST API / Actuator | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Camunda Cockpit | http://localhost:8080/camunda | `beeatlas` / `beeatlas` |
| RabbitMQ Management | http://localhost:15672 | `guest` / `guest` |
| PostgreSQL | `localhost:5432` | БД `fdmbpm`, пользователь `postgres` / `postgres` |

Остановка с удалением volumes:

```bash
docker compose down -v
```

### Состав `docker-compose.yml`

| Контейнер | Описание |
|-----------|----------|
| `fdm-bpm-postgres` | PostgreSQL 15, init-схема `processes` и таблица для git-datasource |
| `rabbitmq` | RabbitMQ 3 с management UI, предобъявление очередей BPM |
| `fdm-bpm-backend` | Сборка из `Dockerfile`, профиль настроек через переменные окружения |

Сеть: `beeatlas-network` (bridge).

## Переменные окружения (Docker Compose)

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `FDM_BPM_SERVICE_PORT` | `8080` | Порт приложения на хосте |
| `FDM_BPM_POSTGRES_DB` | `fdmbpm` | Имя БД |
| `FDM_BPM_POSTGRES_USER` | `postgres` | Пользователь PostgreSQL |
| `FDM_BPM_POSTGRES_PASSWORD` | `postgres` | Пароль PostgreSQL |
| `FDM_BPM_POSTGRES_HOST` | `fdm-bpm-postgres` | Хост БД (внутри compose) |
| `FDM_BPM_POSTGRES_NODEPORT` | `5432` | Порт PostgreSQL на хосте |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `guest` / `guest` | Учётные данные RabbitMQ |
| `RABBITMQ_HOST` | `rabbitmq` | Хост RabbitMQ |
| `RABBITMQ_EXCHANGE` | `fdm.bpm.exchange` | Direct exchange |
| `RABBITMQ_ROUTING_KEY` | `fdm.bpm` | Routing key |
| `INTEGRATION_*_SERVER_URL` | см. compose | URL внешних сервисов |

Полный список переменных для приложения — в секции `environment` сервиса `fdm-bpm-backend` в `docker-compose.yml`.

### Очереди RabbitMQ

При старте compose объявляются durable-очереди:

- `package_queue`
- `tc_description_quality`
- `result_local_graph`
- `result_global_graph`
- `user_drop_cache`

Fanout-exchange для пересчётов: `TECH_CAPABILITY` (задаётся в `application.yml`).

## Локальная разработка (без Docker)

Требования: JDK 17, Maven 3.8+, PostgreSQL, RabbitMQ.

```bash
mvn clean package -DskipTests
java -jar target/fdm-bpm-1.6.2.jar
```

Параметры подключения к БД и RabbitMQ, а также URL интеграций задаются через `application.yml` или переменные окружения Spring Boot (`SPRING_DATASOURCE_*`, `SPRING_RABBITMQ_*`, `INTEGRATION_*`).

Пример для одного PostgreSQL на localhost:

```bash
export SPRING_DATASOURCE_CAMUNDA_URL=jdbc:postgresql://localhost:5432/fdmbpm
export SPRING_DATASOURCE_PROCESSES_URL=jdbc:postgresql://localhost:5432/fdmbpm
export SPRING_DATASOURCE_GIT_URL=jdbc:postgresql://localhost:5432/fdmbpm
export SPRING_DATASOURCE_CAMUNDA_USERNAME=postgres
export SPRING_DATASOURCE_CAMUNDA_PASSWORD=postgres
# ... аналогично для PROCESSES и GIT
export SPRING_RABBITMQ_HOST=localhost
```

Перед первым запуском создайте схему `processes` и таблицу `public.v_fdm_gitlab_languages` (см. init SQL в `docker-compose.yml`, секция `configs`).

## Сборка образа

```bash
docker build -t fdm-bpm:local .
```

Образ: multi-stage (Maven + Temurin 17 JRE), приложение запускается от пользователя `appuser`, healthcheck на `/actuator/health`.

## Основные HTTP API

| Метод | Путь | Назначение |
|-------|------|------------|
| `POST` | `/import-process` | Импорт из Excel |
| `POST` | `/export-process` | Экспорт в Excel |
| `POST` | `/application-process` | Заявочный процесс |
| `GET` | `/application-process/{applicationId}/{type}` | Статус заявки |
| `GET` | `/processes/{id}` | Процесс Camunda по id |
| `GET` | `/processes/context/{name}/{value}` | Контекст процесса |
| `GET` | `/status/{id_enum}/{id}` | История статусов |
| `GET` | `/process-status` | Статусы для pipeline |

Полный список — в контроллерах `controller/` и Swagger UI.

## BPMN-процессы

Диаграммы в `src/main/resources/bpmn/`:

- `import-process`, `export-process`
- `capability_upload`, `bc_order`
- `CMDB-all`, `CMDB-local`
- `user-product-all`, `user-product-local`
- `relations_from_git`, `mapic-interface-upload`, `upload-product-for-mapic`
- `tc_quality`, `calculation_of_criteria`, `clean-S3`, `datapipe-camunda` и др.

Таймеры Camunda по умолчанию отключены на далёкий интервал (`fdm.camunda.timer.*` в `application.yml`) — для dev/prod их переопределяют через конфигурацию.

## Мониторинг

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

## CI

- **Java CI** (`.github/workflows/maven.yml`) — `mvn clean verify` / `mvn package`
- **Smoke test** (`.github/workflows/compose-smoke.yml`) — `docker compose up --wait` на PR

## Структура репозитория

```
├── Dockerfile              # Сборка и runtime-образ приложения
├── docker-compose.yml      # Postgres + RabbitMQ + fdm-bpm-backend
├── pom.xml
└── src/main/
    ├── java/ru/beeline/fdmbpm/
    │   ├── controller/     # REST API
    │   ├── service/        # Бизнес-логика и Camunda delegates
    │   ├── config/         # Datasource, RabbitMQ, Flyway
    │   └── client/         # HTTP-клиенты интеграций
    └── resources/
        ├── application.yml
        ├── bpmn/           # BPMN-диаграммы
        └── db/migration/   # Flyway-миграции (схема processes)
```
