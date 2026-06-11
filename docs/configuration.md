# Configuration

Configuration is environment-driven with safe defaults for local development where possible.

## Shared variables

| Variable | Purpose | Typical Docker value |
| --- | --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address. | `kafka:9092` |
| `KAFKA_TOPIC` | Main normalized event topic. | `h2train.events.v1` |
| `H2TRAIN_STORAGE_ROOT` | Shared storage root for deployments. | `/var/lib/h2train` |
| `DATALAKE_ROOT_PATH` | Datalake root path. | `/var/lib/h2train/datalake` |

Local defaults resolve from the repository root:

```text
H2TRAIN_DB_URL=jdbc:h2:file:./runtime/local/database/h2train;...
DATALAKE_ROOT_PATH=./runtime/local/datalake
```

## Portal variables

| Variable | Purpose |
| --- | --- |
| `APP_PERSISTENCE_TYPE` | Persistence implementation, normally `jdbc`. |
| `APP_BUS_TYPE` | Event bus publisher type, for example `kafka` or `logging`. |
| `H2TRAIN_DB_URL` | JDBC URL for the portal and daemon database. |
| `STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_REDIRECT_URI` | Strava OAuth configuration. |
| `FITBIT_ENABLED`, `FITBIT_CLIENT_ID`, `FITBIT_CLIENT_SECRET`, `FITBIT_REDIRECT_URI` | Fitbit OAuth configuration. |
| `GOOGLE_LOGIN_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | Google login configuration. |
| `PASSWORD_RESET_EMAIL_ENABLED` | Enables SMTP password reset delivery. |
| `PASSWORD_RESET_EMAIL_FROM` | Sender address for reset emails. |
| `SPRING_MAIL_*` | SMTP settings used by Spring Mail. |

## Daemon variables

The daemon needs database access, provider OAuth settings, sync tuning and event bus settings. It should not contain portal-only UI variables.

## Datalake variables

The datalake writer needs only the datalake root and Kafka consumer settings:

- `DATALAKE_ROOT_PATH`
- `APP_BUS_CONSUMER_TYPE`
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_TOPIC`
- `DATALAKE_KAFKA_GROUP_ID`
- `DATALAKE_KAFKA_CLIENT_ID`

## Data API variables

The data API needs only read-model storage and its runtime port:

- `DATALAKE_ROOT_PATH`
- `LONGITUDINAL_DATAMART_PATH`
- `SERVER_PORT`
- projection consumer variables when the time-series projection runs in this module

## Secret policy

Real secrets must not be committed. Use:

```text
deploy/env/*.env
```

for local/deployment values and keep only sanitized examples in:

```text
deploy/env.example/*.env.example
```
