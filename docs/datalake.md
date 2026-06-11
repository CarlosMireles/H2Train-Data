# Datalake

The event datalake stores normalized provider events and remains the source of truth.

## Purpose

The datalake keeps append-oriented event records produced by provider synchronization. It should not be optimized for direct UI/API queries; query-oriented access belongs in datamarts.

## Expected structure

```text
runtime/local/datalake/
  events/
  datamarts/
```

Deployment storage:

```text
/var/lib/h2train/datalake/
  events/
  datamarts/
```

## Events

Relevant normalized event names include:

- `Workout`
- `ActivitySummary`
- `Steps`
- `Distance`
- `CaloriesBurned`
- `HeartRate`
- `BodyComposition`
- `Sleep`

## Operational rules

- Do not delete or rewrite historical events during normal ingestion.
- Do not use high-frequency provider streams unless explicitly implemented in a future phase.
- Keep generated event files out of Git.
- If older generated `dataset/` folders exist, keep them as legacy artifacts and migrate or rebuild into `datamarts/` deliberately.
