# Datalake

El datalake almacena los eventos normalizados de los proveedores y permanece
como fuente de verdad.

## Propósito

El datalake conserva registros orientados a anexado producidos por la
sincronización de proveedores. No debe optimizarse para consultas directas de
la interfaz o de la API; el acceso orientado a consulta corresponde a los
datamarts.

## Estructura esperada

```text
runtime/local/datalake/
  events/
  datamarts/
```

Almacenamiento de despliegue:

```text
/var/lib/h2train/datalake/
  events/
  datamarts/
```

## Eventos

Los nombres de eventos normalizados relevantes incluyen:

- `Workout`
- `ActivitySummary`
- `Steps`
- `Distance`
- `CaloriesBurned`
- `HeartRate`
- `BodyComposition`
- `Nutrition`
- `Sleep`
- `BloodGlucose`
- `Electrocardiogram`
- `AnomalyDetected`

## Reglas operativas

- No eliminar ni reescribir eventos históricos durante la ingesta normal.
- No utilizar flujos de alta frecuencia de los proveedores salvo que se
  implementen explícitamente en una fase futura.
- Mantener fuera de Git los archivos de eventos generados.
- Si existen carpetas antiguas `dataset/`, conservarlas como artefactos
  heredados y migrarlas o reconstruirlas deliberadamente dentro de
  `datamarts/`.
