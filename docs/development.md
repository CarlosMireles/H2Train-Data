# Development

## Requirements

- Java 17
- Maven wrapper or local Maven
- Docker Desktop for containerized execution
- Provider OAuth credentials when testing real provider flows

## Local Maven build

Compile all modules:

```powershell
mvn clean compile
```

Run verification when time allows:

```powershell
mvn clean verify
```

## Running modules locally

This hybrid workflow runs Kafka in Docker and the Spring Boot modules from separate IntelliJ terminals.

Start Kafka from the repository root:

```powershell
docker compose -f deploy/docker-compose.yml up -d kafka
```

Each application terminal must load its own environment because PowerShell environment variables are process-local.

Portal terminal:

```powershell
. .\scripts\load-local-env.ps1 portal
mvn -pl h2train-portal -am spring-boot:run
```

Daemon terminal:

```powershell
. .\scripts\load-local-env.ps1 daemon
mvn -pl h2train-daemon -am spring-boot:run
```

Datalake consumer terminal:

```powershell
. .\scripts\load-local-env.ps1 datalake
mvn -pl h2train-datalake -am spring-boot:run
```

Data API terminal:

```powershell
. .\scripts\load-local-env.ps1 data-api
mvn -pl h2train-data-app -am spring-boot:run
```

To run portal or daemon without persistent repositories:

```powershell
. .\scripts\load-local-env.ps1 portal -Persistence memory
```

The loader reads credentials and service settings from `deploy/env/*.env`, then applies local addresses and storage paths. It does not print secrets.

Inspect Kafka events:

```powershell
docker compose -f deploy/docker-compose.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic h2train.events.v1 --from-beginning
```

Inspect synchronized provider connections:

```powershell
java -cp "$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./runtime/local/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false" -user sa -sql "SELECT provider_id, athlete_id, sync_enabled, sync_interval, last_synced_at, user_id FROM provider_connections"
```

Inspect users:

```powershell
java -cp "$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./runtime/local/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false" -user sa -sql "SELECT id, email, username, provider_ids, created_at FROM user_accounts"
```

## Runtime data

Generated data should remain outside tracked source files. The common local folders are:

```text
runtime/local/
  database/
  datalake/
    events/
    datamarts/
  backups/
  exports/
  logs/
```

`runtime/` is ignored by Git. Maven run configurations use the repository root as their working directory so every module resolves the same storage paths. IntelliJ run configurations should also use the repository root.

## Development rules

- Keep business logic in services/use cases, not controllers.
- Keep provider events as the write model.
- Keep datamarts as derived read models.
- Do not commit secrets or generated H2 database files.
- Prefer small, behavior-preserving refactors over package-wide moves.
