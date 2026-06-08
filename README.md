# H2Train

This repository is organized as a Maven multi-module project where each module maps to a top-level H2Train component.

## Modules

- `h2train-bus`: event contracts, bus ports, incoming message contracts, and Kafka adapters
- `h2train-provider-sync`: shared provider connectors, sync use cases, repositories, schema, and provider HTTP configuration
- `h2train-daemon`: Spring Boot background worker that polls due provider connections and publishes collected events
- `h2train-datalake`: bus event ingester that writes events to the local datalake as JSON Lines
- `h2train-data-app`: Kafka-backed longitudinal time-series projection consumer and read-only query API
- `h2train-portal`: Spring Boot web application, portal UI, account/session logic, OAuth callbacks, sync settings, and static assets

The bus module owns both the stable event/bus contracts and the current Kafka adapter. Core application code still depends on bus interfaces such as `EventPublisher` and `BusMessageHandler`, so another transport can be added inside the Bus component without changing Portal, Daemon, Datalake, or Data App use cases. The provider sync module is a shared library used by the portal and daemon; the portal owns user-facing OAuth/account workflows, while the daemon owns scheduled provider synchronization. The datalake module exposes a `BusMessageHandler` and keeps parsing, writing, and dead-letter handling behind bus-agnostic ingestion code. The data app module consumes the same Kafka events as a projection consumer, updates longitudinal datamarts incrementally, and exposes a read-only API over those datamarts.

Event payloads are anonymized before publication. Personal fields such as email, username, first name, last name, full name, display name, and provider athlete username are replaced with `[ANONYMIZED]`; credentials and token-like fields are replaced with `[REDACTED]`.

## Current scope

- Register and sign in to an internal H2Train account with username, email, and password
- Sign in or register through Google OAuth when Google login is configured
- Redirect a user to a provider OAuth login
- Receive the OAuth callback code and redirect the browser back to the portal
- Exchange the code for provider tokens
- Store the provider connection behind a repository port
- Link several provider connections to the same internal H2Train user account
- Remember whether automatic sync is enabled for each connected athlete
- Let the user choose a sync interval of every 5 hours, every 24 hours, or every 7 days
- Persist connected accounts, sync preferences, and per-event sync state in a local H2 database
- Sync provider events through event collectors
- Collect provider events using a shared ontology grouped as `USER_STATE`, `ACTIVITY`, `PHYSIOLOGICAL`, `BODY_COMPOSITION`, and `HEALTH`
- Publish internal H2Train account events for user registration/login and provider-account synchronization
- Publish collected events through an `EventPublisher` port, currently backed by a logging adapter
- Consume bus events through the datalake ingestion service and persist them to a simple local datalake
- Maintain user-level daily and weekly longitudinal time-series datamarts from normalized event summaries
- Run scheduled provider synchronization in `h2train-daemon`, reusing stored sync cursors
- Stop after event collection, returning batches to the caller and updating sync state

The code is structured around clean ports and provider adapters so new sources such as Garmin or Polar can be added without changing the core use cases. Event collection is now modeled generically, so the system can grow beyond activities. Replaceable concerns such as password hashing, provider catalogs, external identity login, authenticated sessions, bus publishing/consumption, and datalake sinks are behind interfaces.

## Internal account linking

- Users must first create or sign in to an internal account
- Internal accounts store `userId`, `email`, `username`, a PBKDF2 password hash, and linked provider IDs
- Google login creates or reuses the internal account by verified email
- Every `ProviderConnection` carries the authenticated internal `userId`
- Provider authorizations started from the portal link Strava/Fitbit to the current internal account
- Event payloads keep their provider-native `athleteId`; the cross-provider relationship lives in the connection model through `userId`

## Normalized event model

- `USER_STATE`: snapshot events such as `UserProfile` and `UserGoals`
- `ACTIVITY`: incremental `Workout` events plus snapshot `ActivitySummary` events when the provider exposes them
- `PHYSIOLOGICAL`: daily events such as `Steps`, `Distance`, `CaloriesBurned`, and `HeartRate`
- `BODY_COMPOSITION`: snapshot events such as `BodyComposition` and `Nutrition`
- `HEALTH`: snapshot events such as `Sleep`, `BloodGlucose`, `Electrocardiogram`, and `AnomalyDetected`
- `USER_ACCOUNT`: internal H2Train account events such as `user_registered` and `user_logged_in`
- `ACCOUNT_SYNC`: internal H2Train account-linking events such as `provider_account_synced`

Internal H2Train events use `providerId=h2train` so they share the same bus envelope and datalake layout as provider events.
- `BaseEvent`: shared base event with `timestamp`, `sourceSystem`, and `athleteId`
- Every normalized event exposes those base fields directly in the event root:
  - `timestamp`: event timestamp
  - `sourceSystem`: source system (`strava` or `fitbit`)
  - `athleteId`: athlete identifier used by this application
- Event-specific fields are flattened directly into each `ProviderEvent`; there is no `normalizedPayload` wrapper
- `UserProfile` is restricted to `weight`, `height`, `gender`, and `timezone` on top of `BaseEvent`

Only `ACTIVITY` uses a sync cursor, so snapshot categories do not overwrite the incremental activity cursor.

## Required environment variables

- `STRAVA_CLIENT_ID`
- `STRAVA_CLIENT_SECRET`
- `STRAVA_REDIRECT_URI`
- `FITBIT_ENABLED`
- `FITBIT_CLIENT_ID`
- `FITBIT_CLIENT_SECRET`
- `FITBIT_REDIRECT_URI`
- `GOOGLE_LOGIN_ENABLED` (optional, default `false`)
- `GOOGLE_CLIENT_ID` (required only when Google login is enabled)
- `GOOGLE_CLIENT_SECRET` (required only when Google login is enabled)
- `GOOGLE_REDIRECT_URI` (optional, default `http://localhost:8080/auth/google/callback`)
- `PASSWORD_RESET_EMAIL_ENABLED` (optional, default `false`; set `true` to send password recovery emails through SMTP)
- `PASSWORD_RESET_EMAIL_FROM` (optional, default `no-reply@h2train.local`)
- `PASSWORD_RESET_EMAIL_SUBJECT` (optional, default `Reset your H2Train password`)
- `SPRING_MAIL_HOST` (required when `PASSWORD_RESET_EMAIL_ENABLED=true`)
- `SPRING_MAIL_PORT` (optional, commonly `587` for STARTTLS)
- `SPRING_MAIL_USERNAME` (required when your SMTP provider requires authentication)
- `SPRING_MAIL_PASSWORD` (required when your SMTP provider requires authentication)
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` (optional, commonly `true`)
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` (optional, commonly `true`)
- `SYNC_POLL_INTERVAL_MS` (optional, default `60000`; used by `h2train-daemon`)
- `SYNC_CONNECTION_PARALLELISM` (optional, default `4`)
- `SYNC_ACTIVITY_PARALLELISM` (optional, default `2`)
- `SYNC_METRICS_PARALLELISM` (optional, default `2`)
- `PROVIDER_HTTP_CONNECT_TIMEOUT` (optional, default `5s`)
- `PROVIDER_HTTP_READ_TIMEOUT` (optional, default `30s`)
- `APP_PERSISTENCE_TYPE` (optional, default `jdbc`; use `memory` for non-persistent repositories)
- `H2TRAIN_DB_URL` (optional, default `jdbc:h2:file:./data/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false` when running the portal or daemon with Maven)
- `H2TRAIN_DB_USERNAME` (optional, default `sa`)
- `H2TRAIN_DB_PASSWORD` (optional, default empty)
- `APP_BUS_TYPE` (optional, default `logging`; use `kafka` to publish to Kafka)
- `KAFKA_BOOTSTRAP_SERVERS` (optional, default `localhost:9092`)
- `KAFKA_TOPIC` (optional, default `h2train.events.v1`)
- `KAFKA_CLIENT_ID` (optional, default `h2train-portal` for the portal and `h2train-daemon` for the daemon)
- `KAFKA_TOPIC_PARTITIONS` (optional, default `3`)
- `KAFKA_TOPIC_REPLICATION_FACTOR` (optional, default `1`)
- `KAFKA_REQUEST_TIMEOUT` (optional, default `10s`)
- `KAFKA_DELIVERY_TIMEOUT` (optional, default `20s`)
- `KAFKA_MAX_BLOCK` (optional, default `5s`)
- `DATALAKE_ROOT_PATH` (optional, default `../datalake` when running `h2train-datalake`)
- `LONGITUDINAL_DATAMART_PATH` (optional, default `datamarts/longitudinal` under `DATALAKE_ROOT_PATH` when running `h2train-data-app`)
- `APP_BUS_CONSUMER_TYPE` (optional, default `kafka` for `h2train-datalake`; `APP_DATALAKE_BUS_TYPE` is still accepted as a compatibility fallback)
- `DATALAKE_KAFKA_GROUP_ID` (optional, default `h2train-datalake`)
- `DATALAKE_KAFKA_CLIENT_ID` (optional, default `h2train-datalake`)
- `DATALAKE_KAFKA_AUTO_OFFSET_RESET` (optional, default `earliest`)
- `APP_DATA_APP_BUS_TYPE` (optional, default `kafka` for `h2train-data-app`)
- `TIMESERIES_KAFKA_GROUP_ID` (optional, default `h2train-timeseries-projection`)
- `TIMESERIES_KAFKA_CLIENT_ID` (optional, default `h2train-timeseries-projection`)
- `TIMESERIES_KAFKA_AUTO_OFFSET_RESET` (optional, default `earliest`)
- `TIMESERIES_REBUILD_ON_STARTUP` (optional, default `false`; set `true` only to rebuild `datamarts/longitudinal` from `events/` during recovery)

`STRAVA_CLIENT_ID` must be your numeric Strava application ID, not the app name.
`STRAVA_REDIRECT_URI` must match the callback URL configured in your Strava application settings.
The backend now fails at startup if `STRAVA_CLIENT_ID` or `STRAVA_CLIENT_SECRET` are missing.
Set `FITBIT_ENABLED=true` to register the Fitbit provider. Fitbit remains disabled unless that flag is enabled.
Strava user-state snapshots require `profile:read_all`. Fitbit body-related endpoints use the `weight` scope.
Set `GOOGLE_LOGIN_ENABLED=true` and configure the Google OAuth client values to show "Continue with Google" on the login and registration pages. Google login is only used by `h2train-portal`.

## Password recovery email

The portal can send password recovery links through SMTP. By default, SMTP is disabled and recovery links are written to the application log for local development only.

Enable real email delivery:

```powershell
$env:PASSWORD_RESET_EMAIL_ENABLED="true"
$env:PASSWORD_RESET_EMAIL_FROM="no-reply@your-domain.test"
$env:PASSWORD_RESET_EMAIL_SUBJECT="Reset your H2Train password"
$env:SPRING_MAIL_HOST="smtp.your-provider.test"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="smtp-user"
$env:SPRING_MAIL_PASSWORD="smtp-password"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
```

When SMTP is enabled, password reset tokens are still stored only as hashes, expire after 30 minutes, and can be used once.

## Persistence

By default, the portal and daemon share a local H2 file database for internal user accounts, connected provider accounts, user sync preferences, and sync cursors. The Maven Spring Boot plugin starts both executable modules from the repository root, so the default local database is created at:

```text
data/h2train.mv.db
```

This means connected Strava/Fitbit accounts and their sync settings survive application restarts and are visible to both `h2train-portal` and `h2train-daemon`. The schema is initialized from `h2train-provider-sync/src/main/resources/schema.sql`.

To force a specific database path:

```powershell
$env:H2TRAIN_DB_URL="jdbc:h2:file:./data/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false"
```

When running from IntelliJ instead of Maven, set the run configuration working directory to the repository root, or set `H2TRAIN_DB_URL` to an absolute path under the repository `data` directory.

For temporary in-memory execution:

```powershell
$env:APP_PERSISTENCE_TYPE="memory"
```

## Run locally

```powershell
$env:STRAVA_CLIENT_ID="your-client-id"
$env:STRAVA_CLIENT_SECRET="your-client-secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/auth/strava/callback"
$env:FITBIT_ENABLED="true"
$env:FITBIT_CLIENT_ID="your-fitbit-client-id"
$env:FITBIT_CLIENT_SECRET="your-fitbit-client-secret"
$env:FITBIT_REDIRECT_URI="http://localhost:8080/auth/fitbit/callback"
$env:GOOGLE_LOGIN_ENABLED="false"
# Optional when GOOGLE_LOGIN_ENABLED=true:
# $env:GOOGLE_CLIENT_ID="your-google-client-id"
# $env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
# $env:GOOGLE_REDIRECT_URI="http://localhost:8080/auth/google/callback"
# Optional real password recovery email:
# $env:PASSWORD_RESET_EMAIL_ENABLED="true"
# $env:PASSWORD_RESET_EMAIL_FROM="no-reply@your-domain.test"
# $env:SPRING_MAIL_HOST="smtp.your-provider.test"
# $env:SPRING_MAIL_PORT="587"
# $env:SPRING_MAIL_USERNAME="smtp-user"
# $env:SPRING_MAIL_PASSWORD="smtp-password"
# $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
# $env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
$env:SYNC_CONNECTION_PARALLELISM="4"
$env:SYNC_ACTIVITY_PARALLELISM="2"
$env:SYNC_METRICS_PARALLELISM="2"
$env:PROVIDER_HTTP_CONNECT_TIMEOUT="5s"
$env:PROVIDER_HTTP_READ_TIMEOUT="30s"
$env:APP_BUS_TYPE="logging"
mvn -pl h2train-portal -am spring-boot:run
```

Run the provider daemon in another terminal with the same provider, database, and bus environment:

```powershell
$env:STRAVA_CLIENT_ID="your-client-id"
$env:STRAVA_CLIENT_SECRET="your-client-secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/auth/strava/callback"
$env:FITBIT_ENABLED="true"
$env:FITBIT_CLIENT_ID="your-fitbit-client-id"
$env:FITBIT_CLIENT_SECRET="your-fitbit-client-secret"
$env:FITBIT_REDIRECT_URI="http://localhost:8080/auth/fitbit/callback"
$env:APP_BUS_TYPE="logging"
mvn -pl h2train-daemon -am spring-boot:run
```

To publish collected events to Kafka instead of logging them, enable Kafka in both the portal and daemon terminals:

```powershell
docker compose up -d kafka
$env:APP_BUS_TYPE="kafka"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_TOPIC="h2train.events.v1"
# Portal terminal:
$env:KAFKA_CLIENT_ID="h2train-portal"
mvn -pl h2train-portal -am spring-boot:run

# Daemon terminal:
$env:KAFKA_CLIENT_ID="h2train-daemon"
mvn -pl h2train-daemon -am spring-boot:run
```

The application creates the configured Kafka topic automatically when the Kafka bus is enabled. For local development, the topic uses 3 partitions and replication factor 1 by default.

Useful Kafka checks:

```powershell
docker exec -it h2train-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec -it h2train-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic h2train.events.v1 --from-beginning
```

## Datalake ingestion

The datalake writer runs as a separate Spring Boot application. Its core ingestion service accepts generic bus messages and appends valid event envelopes unchanged to JSON Lines files. Kafka is the current input adapter.

Run event producers with Kafka enabled. The portal publishes account/linking events and manual sync events; the daemon publishes scheduled provider sync events.

```powershell
docker compose up -d kafka
$env:APP_BUS_TYPE="kafka"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_TOPIC="h2train.events.v1"
$env:KAFKA_CLIENT_ID="h2train-portal"
mvn -pl h2train-portal -am spring-boot:run
```

In another terminal:

```powershell
$env:APP_BUS_TYPE="kafka"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_TOPIC="h2train.events.v1"
$env:KAFKA_CLIENT_ID="h2train-daemon"
mvn -pl h2train-daemon -am spring-boot:run
```

Run the datalake consumer in another terminal:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_TOPIC="h2train.events.v1"
mvn -pl h2train-datalake -am spring-boot:run
```

The default datalake layout is:

```text
datalake/
  events/
    {providerId}/
      {eventType}/
        events.jsonl
  dead-letter/
    year={yyyy}/
      month={MM}/
        day={dd}/
          failed-events.jsonl
  manifests/
```

Example event path:

```text
datalake/events/strava/ACTIVITY/events.jsonl
datalake/events/h2train/USER_ACCOUNT/events.jsonl
datalake/events/h2train/ACCOUNT_SYNC/events.jsonl
```

If an event cannot be parsed, the consumer writes the original payload plus error metadata to `datalake/dead-letter`.

## Longitudinal time-series datamart

`h2train-data-app` implements the Plan A analytics layer as an event projection. It works only from normalized aggregate events already published to Kafka and stored in `datalake/events`. It does not call Strava activity streams and does not download high-frequency provider samples.

Kafka is the distribution mechanism for both write models and read models:

```text
Provider
  -> Provider Sync
  -> Kafka topic h2train.events.v1
  -> Datalake Writer Consumer writes datalake/events
  -> TimeSeriesProjectionConsumer updates datalake/datamarts/longitudinal
```

CQRS separation:

- `datalake/events/` is the source of truth and write model.
- `datalake/datamarts/longitudinal/` is a materialized read model.
- The API reads exclusively from `datalake/datamarts/longitudinal/`.
- The API does not read `datalake/events/` and does not rebuild projections on request.

The sync interval is operational scheduling, not sampling frequency. A user can sync every 24 hours while the derived series still has `period=P1D` for daily metrics or `period=P1W` for ISO-week activity metrics. `periodStart` and `periodEnd` are calculated from event times such as `timestamp`, `startTime`, or sleep `startTime`, using the subject timezone when a `USER_STATE/UserProfile.timezone` exists. `publishedAt` and repeated sync executions are not used as the temporal period of the data.

`TimeSeriesProjectionConsumer` subscribes to the same Kafka topic as the datalake writer with its own consumer group. For each incoming event, `TimeSeriesProjectionService` transforms only that event, updates only affected metric files, and keeps idempotency state under the longitudinal datamart. Repeated event IDs or unchanged payloads do not duplicate points or aggregates.

Weekly activity metrics are maintained incrementally. When a `Workout` arrives, only the affected ISO week and sport dimensions are updated:

- `weekly_activity_count`
- `weekly_workout_duration`
- `weekly_workout_distance_by_sport`
- `weekly_workout_calories_by_sport`

`TimeSeriesRebuilder` is available as a recovery utility. It can clear and regenerate `datalake/datamarts/longitudinal/` by reading `datalake/events/`, but this is not used during normal operation.

Recovery rebuild command:

```powershell
$env:DATALAKE_ROOT_PATH="datalake"
$env:TIMESERIES_REBUILD_ON_STARTUP="true"
$env:APP_DATA_APP_BUS_TYPE="none"
mvn -pl h2train-data-app -am spring-boot:run
```

Canonical derived point:

```text
TimeSeriesPoint(userId, metricName, period, periodStart, periodEnd, value, unit, provider,
sourceEventType, sourceEventName, aggregationType, activityType, zone, generatedAt)
```

Supported normalized metrics include:

```text
daily_steps
daily_distance
daily_calories
daily_sleep_duration
daily_weight
daily_bmi
daily_body_fat_percentage
daily_activity_count
daily_workout_duration
daily_workout_distance
daily_workout_calories
weekly_activity_count
weekly_workout_duration
weekly_workout_distance_by_sport
weekly_workout_calories_by_sport
heart_rate_zone_minutes
heart_rate_zone_calories
```

The longitudinal datamart layout is inspired by PhysioNet-style subject metadata plus per-subject signal files, but the current content is aggregate event-derived time series, not raw physiological signals:

```text
datalake/
  events/
  datamarts/
    longitudinal/
      subject-info.csv
      timeseries/
        {userId}/
          daily_steps.jsonl
          daily_sleep_duration.jsonl
          weekly_workout_distance_by_sport.jsonl
      activities/
        {userId}/
          activities.jsonl
      sync/
        {userId}/
          status.jsonl
          history.jsonl
      state/
        events/
```

`subject-info.csv` contains non-sensitive subject metadata: `subjectId`, `userId`, `providers`, `timezone`, `gender`, `height`, `weight`, `firstRecord`, and `lastRecord`. If exact age is added later, prefer age groups rather than exact age.

Conceptually, this mirrors the organization of datasets such as PhysioNet or Autonomic Aging by separating subject metadata from time-series files. The important limitation is that H2Train currently stores provider summaries and workouts; it is not yet a high-frequency signal repository. The datamart is near real-time relative to provider synchronization: once a sync publishes an event to Kafka, the projection consumer can update the read model without a batch rebuild.

Run the data API and projection consumer:

```powershell
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
$env:KAFKA_TOPIC="h2train.events.v1"
$env:DATALAKE_ROOT_PATH="datalake"
mvn -pl h2train-data-app -am spring-boot:run
```

Query examples:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}"
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}/metrics"
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}/timeseries?metric=daily_steps&from=2026-04-01&to=2026-04-30"
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}/summary/weekly?from=2026-04-01&to=2026-04-30"
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}/activities?from=2026-04-01&to=2026-04-30&page=0&size=50"
Invoke-RestMethod "http://localhost:8080/api/v1/users/{userId}/dataset/export"
```

Swagger UI and OpenAPI:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## Main endpoints

- `GET /`
- `GET /login`
- `GET /register`
- `POST /account/register`
- `POST /account/login`
- `POST /account/logout`
- `GET /account/me`
- `GET /auth/google/login`
- `GET /auth/google/callback?code=...&state=...`
- `GET /auth/{provider}/login`
- `GET /auth/{provider}/callback?code=...`
- `GET /auth/{provider}/athletes/{athleteId}/sync/{eventType}`
- `GET /auth/{provider}/athletes/{athleteId}/sync-settings`
- `PUT /auth/{provider}/athletes/{athleteId}/sync-settings`
- `GET /auth/health`
- `GET /api/v1/users/{userId}`
- `GET /api/v1/users/{userId}/providers`
- `GET /api/v1/users/{userId}/metrics`
- `GET /api/v1/users/{userId}/timeseries?metric=daily_steps&from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/users/{userId}/timeseries/batch?metrics=daily_steps,daily_sleep_duration&from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/users/{userId}/summary/daily?date=YYYY-MM-DD`
- `GET /api/v1/users/{userId}/summary/weekly?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/users/{userId}/activities?from=YYYY-MM-DD&to=YYYY-MM-DD&activityType=run&page=0&size=50`
- `GET /api/v1/users/{userId}/activities/{activityId}`
- `GET /api/v1/users/{userId}/data-coverage`
- `GET /api/v1/users/{userId}/sync/status`
- `GET /api/v1/users/{userId}/sync/history`
- `GET /api/v1/users/{userId}/dataset/export`
- `GET /api/v1/dataset/metadata`
- `GET /api/v1/dataset/subjects`
- `GET /api/v1/health`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`

The provider authorization and sync setting endpoints require an authenticated internal account session.

## Module layout

- `h2train-bus/src/main/java/com/h2traindata/domain`: shared event contracts such as `BaseEvent`, `EventType`, and `ProviderEvent`
- `h2train-bus/src/main/java/com/h2traindata/bus`: bus ports such as `EventPublisher`, `BusMessageHandler`, and `IncomingBusMessage`
- `h2train-bus/src/main/java/com/h2traindata/infrastructure/bus/kafka`: Kafka bus publisher and consumer adapters
- `h2train-provider-sync/src/main/java/com/h2traindata/application`: shared provider ports, provider registry, and sync use cases
- `h2train-provider-sync/src/main/java/com/h2traindata/domain`: shared account, connection, cursor, sync state, and batch domain model
- `h2train-provider-sync/src/main/java/com/h2traindata/infrastructure`: provider adapters, persistence, provider HTTP config, sync executors, and logging publisher
- `h2train-provider-sync/src/main/resources/schema.sql`: shared H2 schema for portal and daemon persistence
- `h2train-daemon/src/main/java/com/h2traindata/daemon`: scheduler and daemon Spring Boot entrypoint
- `h2train-portal/src/main/java/com/h2traindata/application`: portal account, authorization, identity, and sync settings use cases
- `h2train-datalake/src/main/java/com/h2traindata/datalake`: bus-agnostic ingestion service, datalake sink ports, and JSONL file adapters
- `h2train-data-app/src/main/java/com/h2traindata/dataapp/application`: time-series projection consumer, projection service, rebuilder utility, builder, and query orchestration
- `h2train-data-app/src/main/java/com/h2traindata/dataapp/application/port`: contracts for event parsing, projections, datamart repositories, checkpoints, datalake reads, and rebuilder event repositories
- `h2train-data-app/src/main/java/com/h2traindata/dataapp/infrastructure`: local datalake JSONL reader for rebuilds and longitudinal datamart repository
- `h2train-data-app/src/main/java/com/h2traindata/dataapp/web`: derived time-series REST API
- `h2train-portal/src/main/java/com/h2traindata/web`: HTTP controllers, DTOs, mappers, and portal rendering
- `h2train-portal/src/main/resources/static`: portal UI assets

## Notes

- Fitbit's legacy Web API documentation now warns that this legacy platform is scheduled for deprecation in September 2026, so the Fitbit adapter should be treated as a migration target, not a long-term stable surface.

## Structure at a glance

- `h2train-bus` owns shared event contracts, bus abstractions, and Kafka-specific bus publishing/consumption
- `h2train-provider-sync` owns provider connectors, sync use cases, shared persistence, and the database schema
- `h2train-daemon` owns scheduled provider synchronization as a headless Spring Boot process
- `h2train-datalake` owns bus event ingestion and local datalake writes
- `h2train-data-app` owns the Kafka-backed longitudinal projection and read-only time-series API
- `h2train-portal` owns the browser entrypoint, user-facing web endpoints, OAuth callbacks, account flows, and sync settings

## Next steps

- Implement new providers such as Garmin or Polar behind `ProviderConnector` and `ProviderEventCollector`
- Add optional high-frequency provider streams only after the aggregate Plan A layer is stable
