# H2Train

This repository is now organized as a Maven multi-module project with a clear split between the user-facing portal and the synchronization daemon.

## Modules

- `h2train-daemon`: provider integration, OAuth exchange, sync scheduling, event collection, normalization, and sync state management
- `h2train-portal`: Spring Boot web application, portal UI, user-facing controllers, and static assets

The portal depends on the daemon module and exposes the current end-to-end experience in a single runnable application while keeping both components physically separated in the repository.

## Current scope

- Redirect a user to a provider OAuth login
- Receive the OAuth callback code and redirect the browser back to the portal
- Exchange the code for provider tokens
- Store the provider connection behind a repository port
- Link several provider connections to the same internal H2Train user account
- Remember whether automatic sync is enabled for each connected athlete
- Let the user choose a sync interval of every 5 hours, every 24 hours, or every 7 days
- Sync provider events through event collectors
- Collect provider events using a shared ontology grouped as `USER_STATE`, `ACTIVITY`, `PHYSIOLOGICAL`, `BODY_COMPOSITION`, and `HEALTH`
- Poll due connections on a scheduler and reuse the stored sync cursor
- Stop after event collection, returning batches to the caller and updating sync state

The code is structured around clean ports and provider adapters so new sources such as Garmin or Polar can be added without changing the core use cases. Event collection is now modeled generically, so the system can grow beyond activities.

## Internal account linking

- Every `ProviderConnection` now carries an internal `userId`
- The first OAuth authorization creates that internal account automatically
- Subsequent provider authorizations can reuse the same `userId` and link multiple providers to the same person
- Event payloads keep their provider-native `athleteId`; the cross-provider relationship lives in the connection model through `userId`

## Normalized event model

- `USER_STATE`: snapshot events such as `UserProfile` and `UserGoals`
- `ACTIVITY`: incremental `Workout` events plus snapshot `ActivitySummary` events when the provider exposes them
- `PHYSIOLOGICAL`: daily events such as `Steps`, `Distance`, `CaloriesBurned`, and `HeartRate`
- `BODY_COMPOSITION`: snapshot events such as `BodyComposition` and `Nutrition`
- `HEALTH`: snapshot events such as `Sleep`, `BloodGlucose`, `Electrocardiogram`, and `AnomalyDetected`
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
- `SYNC_CONNECTION_PARALLELISM` (optional, default `4`)
- `SYNC_ACTIVITY_PARALLELISM` (optional, default `2`)
- `SYNC_METRICS_PARALLELISM` (optional, default `2`)
- `PROVIDER_HTTP_CONNECT_TIMEOUT` (optional, default `5s`)
- `PROVIDER_HTTP_READ_TIMEOUT` (optional, default `30s`)

`STRAVA_CLIENT_ID` must be your numeric Strava application ID, not the app name.
`STRAVA_REDIRECT_URI` must match the callback URL configured in your Strava application settings.
The backend now fails at startup if `STRAVA_CLIENT_ID` or `STRAVA_CLIENT_SECRET` are missing.
Set `FITBIT_ENABLED=true` to register the Fitbit provider. Fitbit remains disabled unless that flag is enabled.
Strava user-state snapshots require `profile:read_all`. Fitbit body-related endpoints use the `weight` scope.

## Run locally

```powershell
$env:STRAVA_CLIENT_ID="your-client-id"
$env:STRAVA_CLIENT_SECRET="your-client-secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/auth/strava/callback"
$env:FITBIT_ENABLED="true"
$env:FITBIT_CLIENT_ID="your-fitbit-client-id"
$env:FITBIT_CLIENT_SECRET="your-fitbit-client-secret"
$env:FITBIT_REDIRECT_URI="http://localhost:8080/auth/fitbit/callback"
$env:SYNC_CONNECTION_PARALLELISM="4"
$env:SYNC_ACTIVITY_PARALLELISM="2"
$env:SYNC_METRICS_PARALLELISM="2"
$env:PROVIDER_HTTP_CONNECT_TIMEOUT="5s"
$env:PROVIDER_HTTP_READ_TIMEOUT="30s"
mvn -pl h2train-portal -am spring-boot:run
```

## Main endpoints

- `GET /`
- `GET /auth/{provider}/login`
- `GET /auth/{provider}/callback?code=...`
- `GET /auth/{provider}/athletes/{athleteId}/sync/{eventType}`
- `GET /auth/{provider}/athletes/{athleteId}/sync-settings`
- `PUT /auth/{provider}/athletes/{athleteId}/sync-settings`
- `GET /auth/health`

## Module layout

- `h2train-daemon/src/main/java/com/h2traindata/application`: use cases, ports, and daemon orchestration
- `h2train-daemon/src/main/java/com/h2traindata/domain`: shared domain model
- `h2train-daemon/src/main/java/com/h2traindata/infrastructure`: provider adapters, persistence, and daemon configuration
- `h2train-portal/src/main/java/com/h2traindata/web`: HTTP controllers, DTOs, mappers, and portal rendering
- `h2train-portal/src/main/resources/static`: portal UI assets

## Notes

- Fitbit's legacy Web API documentation now warns that this legacy platform is scheduled for deprecation in September 2026, so the Fitbit adapter should be treated as a migration target, not a long-term stable surface.

## Structure at a glance

- `h2train-daemon` owns sync logic and provider integrations
- `h2train-portal` owns the browser entrypoint and user-facing web endpoints
- the portal module wires both pieces together through a dependency on the daemon module

## Next steps

- Implement new providers such as Garmin or Polar behind `ProviderConnector` and `ProviderEventCollector`
- Replace the in-memory repository with a database-backed `ConnectionRepository`
- Persist sync settings and connection state beyond process restarts
