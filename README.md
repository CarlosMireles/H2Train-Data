# H2Train-Data

H2Train-Data is a modular Spring Boot platform for ingesting sports and health provider data, storing normalized events, deriving longitudinal datamarts and exposing read-only data APIs.

## Architecture

```text
Portal web
    -> Portal database
    -> Sync daemon
    -> Kafka bus
    -> Event datalake
    -> Longitudinal datamarts
    -> Read-only data API
```

Key rules:

- `events/` is the source of truth.
- `datamarts/longitudinal/` is a materialized read model.
- Kafka decouples producers and consumers.
- The API reads datamarts only, not raw event files.
- Runtime data is ignored by Git.

## Modules

| Module | Purpose |
| --- | --- |
| `h2train-bus` | Shared event bus abstractions and Kafka support. |
| `h2train-provider-sync` | Provider OAuth, provider clients and synchronization use cases. |
| `h2train-daemon` | Background synchronization scheduler. |
| `h2train-datalake` | Kafka consumer that writes normalized events to the datalake. |
| `h2train-data-app` | REST API over longitudinal datamarts. |
| `h2train-portal` | Spring Boot MVC portal for accounts, providers and settings. |
| `deploy` | Docker Compose scaffold and environment templates. |
| `docs` | Extended project documentation. |

## Documentation

- [Architecture](docs/architecture.md)
- [Modules](docs/modules.md)
- [Configuration](docs/configuration.md)
- [API](docs/api.md)
- [Datalake](docs/datalake.md)
- [Datamarts](docs/datamarts.md)
- [Deployment](docs/deployment.md)
- [Development](docs/development.md)

## Local development

Requirements:

- Java 17
- Maven
- Kafka when running Kafka-backed ingestion/projections
- Provider credentials for real Strava/Fitbit/Google flows

Build all modules:

```powershell
mvn clean compile
```

Run full verification:

```powershell
mvn clean verify
```

Run the portal locally:

```powershell
. .\scripts\load-local-env.ps1 portal
mvn -pl h2train-portal -am spring-boot:run
```

Run the data API locally:

```powershell
. .\scripts\load-local-env.ps1 data-api
mvn -pl h2train-data-app -am spring-boot:run
```

See [Development](docs/development.md) for the complete IntelliJ terminal workflow.

## Docker scaffold

Docker deployment files live under `deploy/`.

Start the local profile:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build
```

Rebuild containers after code changes:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build --force-recreate
```

The shared Docker storage path is:

```text
/var/lib/h2train
```

## Runtime data

Generated local data must not be committed:

```text
runtime/
  local/
    database/
    datalake/
      events/
      datamarts/
    backups/
    exports/
    logs/
```

For Docker, the same concepts are stored inside `/var/lib/h2train/`.

## Environment files

Real environment files are ignored:

```text
deploy/env/*.env
.env
.env.*
```

Versioned examples are stored in:

```text
deploy/env.example/*.env.example
```

Do not commit OAuth secrets, SMTP passwords, refresh tokens, H2 database files or generated datalake content.
