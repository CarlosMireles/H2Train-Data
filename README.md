# Strava TFT

This project is a Spring Boot backend foundation for a provider-driven fitness event ingestion portal.

## Current scope

- Redirect a user to a provider OAuth login
- Receive the OAuth callback code
- Exchange the code for provider tokens
- Store the provider connection behind a repository port
- Sync provider events through event collectors
- Send those events to a pluggable event sink for the datalake

The code is structured around clean ports and provider adapters so new sources such as Garmin or Polar can be added without changing the core use cases. Event collection is now modeled generically, so the system can grow beyond activities.

## Required environment variables

- `STRAVA_CLIENT_ID`
- `STRAVA_CLIENT_SECRET`
- `STRAVA_REDIRECT_URI`
- `FITBIT_ENABLED`
- `FITBIT_CLIENT_ID`
- `FITBIT_CLIENT_SECRET`
- `FITBIT_REDIRECT_URI`

`STRAVA_CLIENT_ID` must be your numeric Strava application ID, not the app name.
`STRAVA_REDIRECT_URI` must match the callback URL configured in your Strava application settings.
The backend now fails at startup if `STRAVA_CLIENT_ID` or `STRAVA_CLIENT_SECRET` are missing.
Set `FITBIT_ENABLED=true` to register the Fitbit provider. Fitbit remains disabled unless that flag is enabled.

## Run locally

```powershell
$env:STRAVA_CLIENT_ID="your-client-id"
$env:STRAVA_CLIENT_SECRET="your-client-secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/auth/strava/callback"
$env:FITBIT_ENABLED="true"
$env:FITBIT_CLIENT_ID="your-fitbit-client-id"
$env:FITBIT_CLIENT_SECRET="your-fitbit-client-secret"
$env:FITBIT_REDIRECT_URI="http://localhost:8080/auth/fitbit/callback"
mvn spring-boot:run
```

## Main endpoints

- `GET /`
- `GET /auth/{provider}/login`
- `GET /auth/{provider}/callback?code=...`
- `GET /auth/{provider}/athletes/{athleteId}/sync/{eventType}`
- `GET /auth/health`

## Architecture

- `application/usecase`: start authorization, handle callback, sync provider events
- `application/port/out`: provider connectors, event collectors, connection repository, event sink
- `domain`: provider connection, athlete profile, event batch, event type, provider event, sync cursor
- `infrastructure/provider/strava`: Strava HTTP client, DTOs, connector, activity collector
- `infrastructure/provider/fitbit`: Fitbit HTTP client, DTOs, connector, activity collector
- `infrastructure/persistence`: in-memory connection repository
- `infrastructure/datalake`: logging event sink

## Next steps

- Implement new providers such as Garmin or Polar behind `ProviderConnector` and `ProviderEventCollector`
- Replace `LoggingEventSink` with a real sink for S3, Azure Data Lake, GCS, or Kafka
- Replace the in-memory repository with a database-backed `ConnectionRepository`
- Add scheduled refresh, cursor persistence, and incremental event sync
