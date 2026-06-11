# Architecture

H2Train-Data is organized as a modular data ingestion platform for sports and health providers.

## Data flow

```text
Portal web
    -> Portal database
    -> Sync daemon
    -> Kafka bus
    -> Event datalake
    -> Longitudinal datamarts
    -> Read-only data API
```

## Architectural rules

- `events/` is the source of truth for provider data already normalized as events.
- `datamarts/longitudinal/` is a materialized read model derived from events.
- Kafka distributes events between independent consumers.
- The datalake writer persists immutable event records.
- The time-series projection updates longitudinal datamarts incrementally.
- The data API reads only from `datamarts/longitudinal/` and must not read directly from `events/`.
- The rebuilder is an operational recovery tool that can regenerate datamarts from events, but it is not the normal runtime path.

## CQRS separation

Write model:

```text
runtime/local/datalake/events/
```

Read model:

```text
runtime/local/datalake/datamarts/longitudinal/
```

This separation keeps provider ingestion append-oriented and makes API queries fast and stable for dashboards, research exports and longitudinal analysis.

## Runtime storage

Local development uses a single generated-data root:

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

Containerized deployment uses the shared storage root:

```text
/var/lib/h2train/
  database/
  datalake/
    events/
    datamarts/
  exports/
  logs/
```

Generated runtime data is intentionally ignored by Git.
