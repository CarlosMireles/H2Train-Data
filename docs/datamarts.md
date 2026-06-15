# Datamarts

Los datamarts longitudinales son modelos de lectura derivados que se construyen
desde el datalake de eventos mediante proyecciones basadas en Kafka.

## Ruta canónica

```text
runtime/local/datalake/datamarts/longitudinal/
  subject-info.csv
  timeseries/
```

Ruta en Docker:

```text
/var/lib/h2train/datalake/datamarts/longitudinal/
```

## Modelo de series temporales

Cada línea JSONL representa un punto temporal con campos como:

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

## Métricas materializadas

La proyección admite actualmente estas métricas longitudinales:

| Métrica | Evento de origen | Periodo | Unidad | Proyección |
| --- | --- | --- | --- | --- |
| `daily_steps` | `PHYSIOLOGICAL / Steps` | Diario | `steps` | Suma |
| `daily_distance` | `PHYSIOLOGICAL / Distance` | Diario | `m` | Suma |
| `daily_calories` | `PHYSIOLOGICAL / CaloriesBurned` | Diario | `kcal` | Suma |
| `daily_calories_ingested` | `BODY_COMPOSITION / Nutrition` | Diario | `kcal` | Suma |
| `daily_water_consumed` | `BODY_COMPOSITION / Nutrition` | Diario | `ml` | Suma |
| `daily_sleep_duration` | `HEALTH / Sleep` | Diario | `s` | Suma |
| `daily_blood_glucose` | `HEALTH / BloodGlucose` | Diario | `mg/dL` | Último valor |
| `daily_weight` | `BODY_COMPOSITION / BodyComposition` | Diario | `kg` | Último valor |
| `daily_bmi` | `BODY_COMPOSITION / BodyComposition` | Diario | `kg/m2` | Último valor |
| `daily_body_fat_percentage` | `BODY_COMPOSITION / BodyComposition` | Diario | `%` | Último valor |
| `daily_activity_count` | `ACTIVITY / Workout` | Diario | `count` | Conteo |
| `daily_workout_duration` | `ACTIVITY / Workout` | Diario | `s` | Suma |
| `daily_workout_distance` | `ACTIVITY / Workout` | Diario | `m` | Suma |
| `daily_workout_calories` | `ACTIVITY / Workout` | Diario | `kcal` | Suma |
| `weekly_activity_count` | `ACTIVITY / Workout` | Semanal | `count` | Conteo |
| `weekly_workout_duration` | `ACTIVITY / Workout` | Semanal/deporte | `s` | Suma |
| `weekly_workout_distance_by_sport` | `ACTIVITY / Workout` | Semanal/deporte | `m` | Suma |
| `weekly_workout_calories_by_sport` | `ACTIVITY / Workout` | Semanal/deporte | `kcal` | Suma |
| `heart_rate_zone_minutes` | `PHYSIOLOGICAL / HeartRate` | Diario/zona | `min` | Suma |
| `heart_rate_zone_calories` | `PHYSIOLOGICAL / HeartRate` | Diario/zona | `kcal` | Suma |

Las dos series de zonas cardiacas permanecen como proyecciones numéricas
independientes para que sus unidades y reglas de agregación sean explícitas.
La API de datasets expone `GET /api/v1/datasets/heart-rate-zones` como una
vista analítica combinada, agrupada por sujeto, fecha local y proveedor. Esta
vista calcula minutos activos, minutos de alta intensidad, calorías activas,
zona activa dominante y porcentajes por zona sin consultar `events/`.

Solo se crea el archivo de una métrica cuando al menos un evento válido
contiene el valor requerido. Por ello, el endpoint de capacidades puede
mostrar únicamente un subconjunto de esta tabla.

## Modelo de proyección

El funcionamiento normal es incremental:

```text
Evento Kafka -> TimeSeriesProjectionConsumer -> TimeSeriesProjectionService -> actualización del datamart
```

La proyección actualiza únicamente los archivos de métricas y los periodos
diarios o semanales afectados. Es idempotente para evitar puntos duplicados
cuando Kafka reproduce eventos o se reinicia un consumidor.

## Reconstructor

El reconstructor puede regenerar `datamarts/longitudinal/` desde `events/` para
recuperar el sistema. Es una herramienta operativa, no una dependencia
periódica del funcionamiento normal.

Cuando se introduce una métrica nueva, es necesario reconstruir el datamart
para derivarla a partir de los eventos históricos ya almacenados.

## Comparación conceptual

Esta organización se inspira en datasets clínicos y fisiológicos que separan
los metadatos del sujeto de sus señales longitudinales. En H2Train-Data, las
series se agregan desde eventos existentes de los proveedores, no desde
señales brutas de alta frecuencia.
