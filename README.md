# capability-backend-service

Backend-сервис (Spring Boot) для модуля Capability. Запускается в контейнере, использует PostgreSQL и RabbitMQ.

## Требования

- Java 17 (если запускаете локально без контейнера)
- Maven (если запускаете локально без контейнера)
- Podman + `podman compose` (Windows) или Docker + Docker Compose

## Быстрый старт (Podman Compose на Windows)

Перейдите в корень репозитория (где лежит `docker-compose.yml`):

```powershell
cd D:\IDEA_project\beeatlas-capability-backend-service
```

Собрать и поднять:

```powershell
podman compose build
podman compose up -d
```

Остановить:

```powershell
podman compose down
```

Посмотреть логи:

```powershell
podman logs -f capability-backend-app
```

Пересоздать контейнеры (когда меняли `docker-compose.yml` / переменные окружения):

```powershell
podman compose down
podman compose up -d --force-recreate
```

## Порты

По умолчанию `docker-compose.yml` публикует порт сервиса:

- `${CAPABILITY_SERVICE_PORT:-8082} -> 8080` (внутри контейнера приложение слушает `8080`)

## Конфигурация

Сервис ожидает настройки через переменные окружения (см. `docker-compose.yml`).

### RabbitMQ (обязательно)

Если не задать `exchange`/`routing-key`/`vhost`, Spring Boot упадёт на старте с ошибкой вида:
`Could not resolve placeholder 'spring.rabbitmq.*'`.

Минимально нужны:

- `SPRING_RABBITMQ_HOST` (в compose сейчас: `shared-rabbitmq`)
- `SPRING_RABBITMQ_PORT` (по умолчанию `5672`)
- `SPRING_RABBITMQ_USERNAME`
- `SPRING_RABBITMQ_PASSWORD`
- `SPRING_RABBITMQ_VIRTUAL_HOST` (по умолчанию `/`)
- `SPRING_RABBITMQ_TEMPLATE_EXCHANGE` (по умолчанию `capability.exchange`)
- `SPRING_RABBITMQ_TEMPLATE_ROUTING_KEY` (по умолчанию `capability.routing`)

Проверить, что переменные реально попали в контейнер (PowerShell):

```powershell
podman exec -it capability-backend-app printenv | findstr SPRING_RABBITMQ
```

Примечание: предупреждения вида `Failed to obtain TTY size: The handle is invalid.` обычно не критичны.
Если мешают — уберите `-t`:

```powershell
podman exec -i capability-backend-app printenv
```

### PostgreSQL (обязательно)

- `SPRING_DATASOURCE_URL` (в compose сейчас: `jdbc:postgresql://shared-postgres:5432/shared_db`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Также используются:

- `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA` (по умолчанию `capability`)
- `SPRING_FLYWAY_DEFAULT_SCHEMA` (по умолчанию `capability`)

### Интеграции (по окружению)

В compose предусмотрены URL интеграционных сервисов:

- `INTEGRATION_NOTIFICATION_SERVER_URL`
- `INTEGRATION_DOCUMENT_SERVER_URL`
- `INTEGRATION_PRODUCT_SERVER_URL`
- `INTEGRATION_BPM_SERVER_URL`
- `INTEGRATION_PACK_LOADER_SERVER_URL`
- `INTEGRATION_DASHBOARD_SERVER_URL`
- `INTEGRATION_AUTH_SERVER_URL`
- `INTEGRATION_AUTHSSO_SERVER_URL`
- `INTEGRATION_AI_TOOL_SERVER_URL`

## Сети (shared infrastructure)

Текущий `docker-compose.yml` подключает сервис к внешней сети:

- `shared-network` (external)
- имя сети: `shared-infrastructure_shared-network`

Это означает, что перед запуском compose должна существовать внешняя сеть и в ней должны быть доступны хосты:

- `shared-postgres`
- `shared-rabbitmq`

Проверить сети:

```powershell
podman network ls
```

## Типовые проблемы

### `Could not resolve placeholder 'spring.rabbitmq.template.exchange'`

Причина: внутри контейнера нет `SPRING_RABBITMQ_TEMPLATE_EXCHANGE` (или `spring.rabbitmq.template.exchange`).

Диагностика:

```powershell
podman exec -it capability-backend-app printenv | findstr SPRING_RABBITMQ
```

Если переменной нет — пересоздайте контейнеры:

```powershell
podman compose down
podman compose up -d --force-recreate
```

## Локальный запуск (без контейнера)

Сборка:

```powershell
mvn clean package -DskipTests
```

Запуск:

```powershell
java -jar .\target\capability-*.jar
```
