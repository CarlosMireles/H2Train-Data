# API

The public data API is versioned under:

```text
/api/v1
```

The API is read-only for longitudinal data and must resolve queries from:

```text
runtime/local/datalake/datamarts/longitudinal/
```

It must not query the event source of truth directly.

## Main resources

| Endpoint | Purpose |
| --- | --- |
| `GET /api/v1/health` | Service healthcheck. |
| `GET /api/v1/users/{userId}` | User/subject metadata from the longitudinal datamart. |
| `GET /api/v1/users/{userId}/providers` | Connected provider summary. |
| `GET /api/v1/users/{userId}/metrics` | Available longitudinal metrics. |
| `GET /api/v1/users/{userId}/timeseries` | Single metric time-series query. |
| `GET /api/v1/users/{userId}/timeseries/batch` | Multiple metric time-series query. |
| `GET /api/v1/users/{userId}/summary/daily` | Daily summary. |
| `GET /api/v1/users/{userId}/summary/weekly` | Weekly activity summary. |
| `GET /api/v1/users/{userId}/activities` | Paginated activity listing from the read model. |
| `GET /api/v1/users/{userId}/activities/{activityId}` | Activity detail from the read model. |
| `GET /api/v1/users/{userId}/data-coverage` | Metric coverage and missing-day information. |
| `GET /api/v1/users/{userId}/sync/status` | Synchronization status exposed without Kafka internals. |
| `GET /api/v1/users/{userId}/sync/history` | Synchronization history exposed without Kafka internals. |
| `GET /api/v1/users/{userId}/dataset/export` | Exportable derived dataset for one user. |
| `GET /api/v1/dataset/metadata` | Dataset-level metadata. |
| `GET /api/v1/dataset/subjects` | Available subjects. |

## Contract guidelines

- Responses are JSON.
- DTOs hide physical datalake paths and Kafka details.
- Query filters should use stable resource parameters such as `metric`, `from`, `to`, `resolution`, `page` and `size`.
- New metrics should be added without breaking existing response shapes.
