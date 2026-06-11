# Datamarts

Longitudinal datamarts are derived read models built from the event datalake through Kafka-based projections.

## Canonical path

```text
runtime/local/datalake/datamarts/longitudinal/
  subject-info.csv
  timeseries/
```

Docker path:

```text
/var/lib/h2train/datalake/datamarts/longitudinal/
```

## Time-series model

Each JSONL line represents one time-series point with fields such as:

- `userId`
- `metricName`
- `period`
- `periodStart`
- `periodEnd`
- `value`
- `unit`
- `provider`
- `sourceEventType`
- `sourceEventName`
- `aggregationType`
- `activityType`
- `generatedAt`

## Projection model

Normal operation is incremental:

```text
Kafka event -> TimeSeriesProjectionConsumer -> TimeSeriesProjectionService -> datamart update
```

The projection updates only affected metric files and affected daily/weekly periods. It must be idempotent to avoid duplicate points when Kafka replays or a consumer restarts.

## Rebuilder

The rebuilder can regenerate `datamarts/longitudinal/` from `events/` for recovery. It is an operational tool, not a scheduled batch dependency.

## Conceptual comparison

This organization is inspired by clinical and physiological dataset layouts where subject metadata and longitudinal signals are separated. In H2Train-Data, the series are aggregated from existing provider events, not raw high-frequency signals.
