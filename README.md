# Strava TFT

This project is now a Spring Boot backend foundation for a Strava data ingestion portal.

## Current scope

- Redirect a user to Strava OAuth login
- Receive the OAuth callback code
- Exchange the code for Strava tokens
- Pull recent activities from the Strava API
- Send those activities to a pluggable datalake writer

## Required environment variables

- `STRAVA_CLIENT_ID`
- `STRAVA_CLIENT_SECRET`
- `STRAVA_REDIRECT_URI`

`STRAVA_REDIRECT_URI` must match the callback URL configured in your Strava application settings.

## Run locally

```powershell
$env:STRAVA_CLIENT_ID="your-client-id"
$env:STRAVA_CLIENT_SECRET="your-client-secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/auth/strava/callback"
mvn spring-boot:run
```

## Main endpoints

- `GET /`
- `GET /auth/strava/login`
- `GET /auth/strava/callback?code=...`
- `GET /auth/strava/health`

## Next steps

- Replace `LoggingDataLakeWriter` with a real sink for S3, Azure Data Lake, or GCS
- Persist athlete connections and refresh tokens in a database
- Add scheduled refresh and incremental activity sync
- Add a frontend portal and user session management
