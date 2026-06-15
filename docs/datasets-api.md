# API de datasets personalizados

La API de datasets permite seleccionar sujetos mediante condiciones analíticas y
exportar sus métricas longitudinales. Todas las consultas se resuelven
exclusivamente desde:

```text
datalake/datamarts/longitudinal/
```

La API no consulta directamente `datalake/events/`.

## Puesta en marcha

Iniciar `h2train-data-app` desde la raíz del proyecto:

```powershell
. .\scripts\load-local-env.ps1 data-api
mvn -pl h2train-data-app -am spring-boot:run
```

Por defecto, la API queda disponible en `http://localhost:8080`.
En el despliegue Docker definido por el proyecto se publica en
`http://localhost:8081`.

Documentación interactiva:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:8080/v3/api-docs`

En Docker se utilizan respectivamente:

- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- Especificación OpenAPI: `http://localhost:8081/v3/api-docs`

## Capacidades disponibles

```http
GET /api/v1/datasets/query/capabilities
```

Devuelve las métricas detectadas en el datamart y las operaciones soportadas:

```json
{
  "metrics": [
    "daily_blood_glucose",
    "daily_calories",
    "daily_calories_ingested",
    "daily_sleep_duration",
    "daily_steps",
    "daily_water_consumed",
    "weekly_activity_count"
  ],
  "operators": ["gt", "gte", "lt", "lte", "eq", "between"],
  "aggregations": ["avg", "sum", "min", "max", "count", "latest"],
  "formats": ["json", "jsonl", "csv"],
  "dimensions": ["zone", "activityType", "provider"]
}
```

Ejemplo:

```powershell
curl.exe "http://localhost:8080/api/v1/datasets/query/capabilities"
```

La lista de métricas no está fijada en el controlador. Se obtiene de las
series disponibles para los sujetos del datamart longitudinal.

## Consulta simple de sujetos

```http
GET /api/v1/datasets/query
```

### Parámetros

| Parámetro | Obligatorio | Descripción |
| --- | --- | --- |
| `metric` | Sí | Métrica longitudinal disponible. |
| `operator` | Sí | `gt`, `gte`, `lt`, `lte`, `eq` o `between`. |
| `value` | Sí | Valor de comparación o límite inferior. |
| `maxValue` | Solo `between` | Límite superior inclusivo. |
| `aggregation` | Sí | `avg`, `sum`, `min`, `max`, `count` o `latest`. |
| `zone` | No | Filtra los puntos por zona cardiaca. Se puede repetir. |
| `activityType` | No | Filtra los puntos por tipo de actividad. Se puede repetir. |
| `provider` | No | Filtra los puntos por proveedor. Se puede repetir. |
| `from` | No | Fecha inicial inclusiva, formato `YYYY-MM-DD`. |
| `to` | No | Fecha final inclusiva, formato `YYYY-MM-DD`. |
| `format` | Sí | `json`, `jsonl` o `csv`. |

La agregación se calcula por separado para cada sujeto usando los puntos que
están dentro del periodo solicitado. Los sujetos sin puntos para la métrica y
el periodo no cumplen la condición.

Ejemplo: media de calorías superior a 2500:

```powershell
curl.exe "http://localhost:8080/api/v1/datasets/query?metric=daily_calories&operator=gt&value=2500&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Respuesta JSON:

```json
{
  "query": {
    "metric": "daily_calories",
    "operator": "gt",
    "value": 2500,
    "maxValue": null,
    "aggregation": "avg",
    "dimensions": {},
    "from": "2026-01-01",
    "to": "2026-06-01",
    "format": "json"
  },
  "subjects": [
    {
      "userId": "u001",
      "metric": "daily_calories",
      "aggregatedValue": 2840.5,
      "from": "2026-01-01",
      "to": "2026-06-01"
    }
  ]
}
```

Ejemplo con `between`, cuyos límites son inclusivos:

```powershell
curl.exe "http://localhost:8080/api/v1/datasets/query?metric=daily_steps&operator=between&value=8000&maxValue=12000&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Ejemplo: sujetos con más de 30 minutos acumulados en zona `cardio`, sin
mezclar los puntos de `peak` ni `fat_burn`:

```powershell
curl.exe "http://localhost:8080/api/v1/datasets/query?metric=heart_rate_zone_minutes&operator=gt&value=30&aggregation=sum&zone=cardio&from=2026-01-01&to=2026-06-01&format=json"
```

Ejemplo CSV:

```powershell
curl.exe -OJ "http://localhost:8080/api/v1/datasets/query?metric=daily_calories&operator=gt&value=2500&aggregation=avg&from=2026-01-01&to=2026-06-01&format=csv"
```

Cabeceras CSV:

```text
userId,metric,aggregatedValue,from,to
```

En JSONL se devuelve una línea por sujeto seleccionado.

## Exportación avanzada

```http
POST /api/v1/datasets/export
Content-Type: application/json
```

El proceso es:

1. Aplicar cada filtro agregado a cada sujeto.
2. Combinar todos los filtros mediante AND.
3. Seleccionar los sujetos que cumplen todos los filtros.
4. Exportar los puntos longitudinales de las métricas solicitadas dentro del
   rango de fechas.

Las agregaciones se utilizan para seleccionar sujetos. Las filas exportadas
son los puntos originales del datamart, conservando periodo, unidad, proveedor
y dimensiones como deporte o zona cardiaca.

Ejemplo de solicitud:

```json
{
  "metrics": [
    "daily_steps",
    "daily_calories"
  ],
  "filters": [
    {
      "metric": "daily_calories",
      "operator": "gt",
      "value": 1400,
      "aggregation": "avg",
      "dimensions": {
        "provider": ["fitbit"]
      }
    }
  ],
  "dimensions": {
    "provider": ["fitbit"]
  },
  "from": "2026-01-01",
  "to": "2026-06-15",
  "format": "csv"
}
```

Las métricas de los ejemplos son orientativas. Antes de construir una
solicitud se debe consultar `/api/v1/datasets/query/capabilities`, porque una
métrica solo está disponible si existe en el datamart longitudinal actual.

El valor de una métrica debe expresarse en la unidad almacenada en el
datamart. Por ejemplo, cuando `daily_sleep_duration` esté disponible y utilice
segundos, seis horas se expresan como `21600`.

Ejemplo con PowerShell:

```powershell
$body = @{
    metrics = @("daily_steps", "daily_calories")
    filters = @(
        @{
            metric = "daily_calories"
            operator = "gt"
            value = 1400
            aggregation = "avg"
        }
    )
    from = "2026-01-01"
    to = "2026-06-15"
    format = "csv"
} | ConvertTo-Json -Depth 5

Invoke-WebRequest `
    -Uri "http://localhost:8080/api/v1/datasets/export" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "h2train-dataset.csv"
```

También se puede exportar sin filtros usando `"filters": []`. En ese caso se
incluyen todos los sujetos disponibles.

Las dimensiones dentro de cada filtro se aplican antes de agregar y sirven
para seleccionar sujetos. Las dimensiones de nivel superior se aplican a las
filas exportadas. Se soportan:

- `zone`, por ejemplo `cardio`, `peak`, `fat_burn` u `out_of_range`.
- `activityType`, por ejemplo `running` o `cycling`.
- `provider`, por ejemplo `fitbit`.

### Respuesta JSON

```json
{
  "request": {
    "metrics": ["daily_steps", "daily_calories"],
    "filters": [],
    "dimensions": {},
    "from": "2026-01-01",
    "to": "2026-06-01",
    "format": "json"
  },
  "subjectCount": 2,
  "rowCount": 120,
  "rows": [
    {
      "userId": "u001",
      "metric": "daily_steps",
      "value": 10340,
      "periodStart": "2026-01-01T00:00:00Z",
      "periodEnd": "2026-01-02T00:00:00Z",
      "unit": "steps",
      "period": "P1D",
      "provider": "fitbit",
      "activityType": null,
      "zone": null
    }
  ]
}
```

En JSONL se devuelve una línea por punto. En CSV se utilizan estas cabeceras:

```text
userId,metric,value,periodStart,periodEnd,unit,period,provider,activityType,zone
```

## Agregaciones

| Agregación | Comportamiento |
| --- | --- |
| `avg` | Media aritmética de los valores no nulos. |
| `sum` | Suma de los valores no nulos. |
| `min` | Valor mínimo. |
| `max` | Valor máximo. |
| `count` | Número de puntos con valor no nulo. |
| `latest` | Valor del punto con `periodStart` más reciente. |

En métricas con dimensiones, como deporte, proveedor o zona cardiaca, la
agregación incluye todos los puntos del periodo salvo que la petición incluya
un filtro `dimensions`.

## Vista analítica de zonas cardiacas

```http
GET /api/v1/datasets/heart-rate-zones
```

Esta vista une `heart_rate_zone_minutes` y `heart_rate_zone_calories` sin
crear una serie con unidades mezcladas. Se agrupa por sujeto, fecha local y
proveedor, evitando sumar datos equivalentes procedentes de dispositivos
distintos.

| Parámetro | Obligatorio | Descripción |
| --- | --- | --- |
| `from` | No | Fecha inicial inclusiva. |
| `to` | No | Fecha final inclusiva. |
| `zone` | No | Limita las zonas devueltas; se puede repetir. |
| `provider` | No | Limita los proveedores; se puede repetir. |
| `format` | Sí | `json`, `jsonl` o `csv`. |

Ejemplo:

```powershell
curl.exe "http://localhost:8080/api/v1/datasets/heart-rate-zones?from=2026-06-01&to=2026-06-15&format=json"
```

Respuesta abreviada:

```json
{
  "query": {
    "from": "2026-06-01",
    "to": "2026-06-15",
    "zones": [],
    "providers": [],
    "format": "json"
  },
  "days": [
    {
      "userId": "u001",
      "date": "2026-06-08",
      "provider": "fitbit",
      "trackedMinutes": 1440,
      "activeMinutes": 42,
      "totalCalories": 2520.4,
      "activeCalories": 410.2,
      "highIntensityMinutes": 17,
      "dominantActiveZone": "fat_burn",
      "zones": [
        {
          "zone": "cardio",
          "minutes": 12,
          "calories": 210.1,
          "percentageOfTrackedTime": 0.83,
          "percentageOfActiveTime": 28.57
        }
      ]
    }
  ]
}
```

`activeMinutes` y `activeCalories` excluyen `out_of_range`.
`highIntensityMinutes` suma las zonas normalizadas `cardio` y `peak`.
`dominantActiveZone` es la zona activa con más minutos.

En JSONL y CSV se devuelve una fila por combinación
`usuario + fecha + proveedor + zona`. Las cabeceras CSV son:

```text
userId,date,provider,trackedMinutes,activeMinutes,totalCalories,activeCalories,highIntensityMinutes,dominantActiveZone,zone,minutes,calories,percentageOfTrackedTime,percentageOfActiveTime
```

## Formatos y cabeceras

| Formato | `Content-Type` |
| --- | --- |
| JSON | `application/json` |
| JSONL | `application/x-ndjson` |
| CSV | `text/csv` |

Las exportaciones incluyen `Content-Disposition` con un nombre de archivo.
Las consultas simples JSON se muestran directamente en la respuesta.

## Errores

Los errores se devuelven sin stack trace:

```json
{
  "error": "maxValue es obligatorio para between"
}
```

| Código | Situación |
| --- | --- |
| `400 Solicitud no válida` | Parámetros, fechas, operador, agregación o formato inválidos. |
| `404 No encontrado` | La métrica solicitada no existe en el datamart. |
| `500 Error interno` | Error inesperado del servidor. |

Validaciones relevantes:

- `from` debe ser anterior o igual a `to`.
- `between` requiere `value` y `maxValue`.
- En `between`, `value` debe ser menor o igual a `maxValue`.
- Todas las métricas solicitadas deben existir.
- Las fechas deben usar el formato ISO `YYYY-MM-DD`.
