# API

La API pública de datos está versionada bajo:

```text
/api/v1
```

La API es de solo lectura para los datos longitudinales y debe resolver las
consultas desde:

```text
runtime/local/datalake/datamarts/longitudinal/
```

No debe consultar directamente la fuente de verdad de eventos.

## Recursos principales

| Endpoint | Propósito |
| --- | --- |
| `GET /api/v1/health` | Comprobación del estado del servicio. |
| `GET /api/v1/users/{userId}` | Metadatos del usuario o sujeto desde el datamart longitudinal. |
| `GET /api/v1/users/{userId}/providers` | Resumen de proveedores conectados. |
| `GET /api/v1/users/{userId}/metrics` | Métricas longitudinales disponibles. |
| `GET /api/v1/users/{userId}/timeseries` | Consulta de una serie temporal. |
| `GET /api/v1/users/{userId}/timeseries/batch` | Consulta conjunta de varias series temporales. |
| `GET /api/v1/users/{userId}/summary/daily` | Resumen diario. |
| `GET /api/v1/users/{userId}/summary/weekly` | Resumen semanal de actividad. |
| `GET /api/v1/users/{userId}/activities` | Listado paginado de actividades del modelo de lectura. |
| `GET /api/v1/users/{userId}/activities/{activityId}` | Detalle de una actividad. |
| `GET /api/v1/users/{userId}/data-coverage` | Cobertura de métricas y días sin observaciones. |
| `GET /api/v1/users/{userId}/sync/status` | Estado de sincronización sin exponer detalles internos de Kafka. |
| `GET /api/v1/users/{userId}/sync/history` | Historial de sincronización sin exponer detalles internos de Kafka. |
| `GET /api/v1/users/{userId}/dataset/export` | Dataset derivado exportable para un usuario. |
| `GET /api/v1/dataset/metadata` | Metadatos generales del dataset. |
| `GET /api/v1/dataset/subjects` | Sujetos disponibles. |
| `GET /api/v1/datasets/query` | Selección de sujetos mediante una métrica longitudinal agregada. |
| `POST /api/v1/datasets/export` | Exportación personalizada para sujetos que cumplen filtros analíticos. |
| `GET /api/v1/datasets/heart-rate-zones` | Vista analítica diaria de minutos y calorías por zona cardiaca. |
| `GET /api/v1/datasets/query/capabilities` | Métricas, operadores, agregaciones, formatos y dimensiones disponibles. |

Los ejemplos detallados de los endpoints de datasets están disponibles en la
[guía de la API de datasets personalizados](datasets-api.md).

`h2train-data-app` expone la documentación interactiva en:

- Ejecución local con Maven:
  `http://localhost:8080/swagger-ui/index.html`
- Despliegue Docker:
  `http://localhost:8081/swagger-ui/index.html`
- Especificación OpenAPI local:
  `http://localhost:8080/v3/api-docs`
- Especificación OpenAPI en Docker:
  `http://localhost:8081/v3/api-docs`

## Directrices del contrato

- Las respuestas usan JSON de forma predeterminada. Los endpoints de datasets
  también admiten JSONL y CSV.
- Los DTO ocultan las rutas físicas del datalake y los detalles de Kafka.
- Los filtros deben utilizar parámetros estables como `metric`, `from`, `to`,
  `resolution`, `page` y `size`.
- Las nuevas métricas deben añadirse sin romper las estructuras de respuesta
  existentes.
