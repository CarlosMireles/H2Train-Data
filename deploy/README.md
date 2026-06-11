# H2Train-Data Docker deployment scaffold

This directory prepares H2Train-Data for progressive Docker Compose deployment. It does not replace local Maven/Spring Boot development and it does not change business logic.

## Current choices and advantages

### 1. Dockerfiles

Choice: one reusable multi-stage Dockerfile at `deploy/docker/Dockerfile.app`, parameterized with `MODULE`.

Advantages:

- Avoids duplicating one Dockerfile per Java module.
- Keeps all services aligned with the same Java runtime and build strategy.
- Builds only the selected Maven module plus its required dependencies using `mvn -pl <module> -am`.
- Produces runtime images without Maven or source code.
- Runs as a non-root `h2train` user.

### 2. Persistence

Choice: keep H2 file-based persistence for the first Docker iteration.

Advantages:

- Preserves current application behavior.
- Avoids adding PostgreSQL driver/dependencies before the application is ready for that migration.
- Keeps the database inside the shared Docker volume under `/var/lib/h2train/database`.
- Minimizes risk while containerizing infrastructure first.

Future PostgreSQL migration remains possible, but should be a separate task because it changes database runtime assumptions.

### 3. Healthchecks

Choice: add Docker healthchecks in Compose.

Advantages:

- Kafka readiness is checked before dependent services start.
- Web services are checked through HTTP endpoints.
- Non-web services are checked by verifying the Java process is alive.
- No application business logic is changed.

### 4. Deployment profiles

Choice: use Docker Compose profiles: `local`, `staging`, `prod`, `apps`, and `tools`.

Advantages:

- `local` can include developer tools like Kafka UI.
- `staging` and `prod` can run application services without Kafka UI by default.
- `apps` remains a direct way to start all H2Train app containers.
- The same compose file can evolve without duplicating service definitions.

Profile examples are stored in `deploy/profiles/`.

### 5. Logs

Choice: keep Docker stdout/stderr and also configure Spring Boot file logs under `/var/lib/h2train/logs/<service>`.

Advantages:

- Docker logs still work with `docker compose logs`.
- Persistent service logs are available in the shared `h2train-storage` volume.
- Each service gets an isolated log directory.
- No code changes are required because Spring Boot maps `LOGGING_FILE_PATH` automatically.

## Layout

```text
deploy/
├── docker-compose.yml
├── docker/
│   └── Dockerfile.app
├── env/
│   ├── common.env
│   ├── portal.env
│   ├── daemon.env
│   ├── datalake.env
│   └── data-api.env
├── env.example/
│   ├── common.env.example
│   ├── portal.env.example
│   ├── daemon.env.example
│   ├── datalake.env.example
│   └── data-api.env.example
└── profiles/
    ├── local.env.example
    ├── staging.env.example
    └── prod.env.example
```

## Runtime storage

All application containers share this volume mount:

```text
/var/lib/h2train/
├── database/
├── datalake/
│   ├── events/
│   └── datamarts/
├── exports/
└── logs/
```

The Compose volume is named `h2train-storage` and is mounted at `/var/lib/h2train`.

## Environment files

`deploy/env/` contains local runtime values for Docker Compose. These files may contain secrets and must not be committed.

`deploy/env.example/` contains safe examples and should remain versioned. Copy from `env.example` to `env` when preparing a new environment.

### common.env

Used by all H2Train services.

Contains shared infrastructure values:

```text
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC=h2train.events.v1
H2TRAIN_STORAGE_ROOT=/var/lib/h2train
DATALAKE_ROOT_PATH=/var/lib/h2train/datalake
H2TRAIN_DB_URL=jdbc:h2:file:/var/lib/h2train/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false
H2TRAIN_DB_USERNAME=sa
H2TRAIN_DB_PASSWORD=
```

### portal.env

Used by `h2train-portal`.

Complete manually:

```text
STRAVA_CLIENT_ID
STRAVA_CLIENT_SECRET
STRAVA_REDIRECT_URI
FITBIT_CLIENT_ID
FITBIT_CLIENT_SECRET
FITBIT_REDIRECT_URI
GOOGLE_LOGIN_ENABLED
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
```

Optional email delivery for password recovery:

```text
PASSWORD_RESET_EMAIL_ENABLED
PASSWORD_RESET_EMAIL_FROM
PASSWORD_RESET_EMAIL_SUBJECT
SPRING_MAIL_HOST
SPRING_MAIL_PORT
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE
```

### daemon.env

Used by `h2train-daemon`.

Contains synchronization and provider-access configuration:

```text
APP_PERSISTENCE_TYPE
APP_BUS_TYPE
LOGGING_FILE_PATH
SYNC_POLL_INTERVAL_MS
SYNC_CONNECTION_PARALLELISM
SYNC_ACTIVITY_PARALLELISM
SYNC_METRICS_PARALLELISM
PROVIDER_HTTP_CONNECT_TIMEOUT
PROVIDER_HTTP_READ_TIMEOUT
STRAVA_CLIENT_ID
STRAVA_CLIENT_SECRET
STRAVA_REDIRECT_URI
FITBIT_ENABLED
FITBIT_CLIENT_ID
FITBIT_CLIENT_SECRET
FITBIT_REDIRECT_URI
FITBIT_INITIAL_ACTIVITY_FETCH_LIMIT
FITBIT_INCREMENTAL_ACTIVITY_FETCH_LIMIT
```

### datalake.env

Used by `h2train-datalake`.

Contains the writer target and Kafka consumer identity:

```text
DATALAKE_ROOT_PATH
APP_BUS_CONSUMER_TYPE
LOGGING_FILE_PATH
DATALAKE_KAFKA_GROUP_ID
DATALAKE_KAFKA_CLIENT_ID
DATALAKE_KAFKA_AUTO_OFFSET_RESET
```

### data-api.env

Used by `h2train-data-app`.

Contains the datamart reader path, API port, and time-series projection consumer identity:

```text
DATALAKE_ROOT_PATH
LONGITUDINAL_DATAMART_PATH
SERVER_PORT
LOGGING_FILE_PATH
APP_DATA_APP_BUS_TYPE
TIMESERIES_KAFKA_GROUP_ID
TIMESERIES_KAFKA_CLIENT_ID
TIMESERIES_KAFKA_AUTO_OFFSET_RESET
TIMESERIES_REBUILD_ON_STARTUP
```

## Commands

Validate Compose configuration:

```powershell
docker compose -f deploy/docker-compose.yml config
docker compose -f deploy/docker-compose.yml --profile apps config
```

Start only Kafka and Kafka UI for local debugging:

```powershell
docker compose -f deploy/docker-compose.yml --profile tools up kafka kafka-ui
```

Build and start all application services for local Docker execution:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up --build
```

Start all app services without local tools:

```powershell
docker compose -f deploy/docker-compose.yml --profile apps up --build
```

## Git policy

Do not commit:

```text
deploy/env/*.env
```

Commit:

```text
.dockerignore
deploy/docker-compose.yml
deploy/docker/Dockerfile.app
deploy/env.example/*.env.example
deploy/profiles/*.env.example
deploy/README.md
```

## Remaining next steps

1. Run a real Docker build once provider secrets are configured locally.
2. Add production-grade secret handling, for example Docker secrets or an external vault.
3. Decide and implement PostgreSQL support if H2 is no longer enough for shared/prod deployments.
4. Add reverse proxy/TLS configuration if exposing services outside localhost.
5. Add CI jobs to build and publish the Docker images.
