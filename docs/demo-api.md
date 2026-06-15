# Demostración de la API de datasets

La plataforma incluye un entorno Docker aislado para demostrar las consultas y
exportaciones de datasets sin mezclar datos sintéticos con cuentas, eventos o
datamarts reales.

## Aislamiento

La demostración utiliza exclusivamente:

```text
Volumen: h2train-data-demo-storage
API:     http://localhost:8082
Swagger: http://localhost:8082/swagger-ui/index.html
```

No monta el volumen habitual `h2train-storage`, no inicia el portal ni el
daemon y desactiva el consumidor Kafka de la API de demostración.

El flujo es:

```text
h2train-demo-data
    -> genera eventos normalizados sintéticos
    -> h2train-data-demo-storage/datalake/events/
    -> h2train-demo-api reconstruye el datamart
    -> h2train-data-demo-storage/datalake/datamarts/longitudinal/
```

Los datamarts nunca se generan directamente. El contenedor auxiliar crea los
eventos y la API utiliza el reconstructor existente.

## Datos generados

El generador utiliza una semilla fija y crea:

- 12 sujetos, desde `demo-u001` hasta `demo-u012`.
- Datos entre el 1 de enero y el 1 de junio de 2026.
- Actividad diaria, pasos, distancia y calorías.
- Sueño, nutrición, agua, glucosa y composición corporal.
- Entrenamientos de carrera, ciclismo, fuerza y paseo.
- Minutos y calorías en las zonas `out_of_range`, `fat_burn`, `cardio` y
  `peak`.
- Ausencias controladas de observaciones, especialmente para `demo-u011`.

El manifiesto generado se almacena dentro del volumen en:

```text
/var/lib/h2train/demo/manifest.json
```

## Arranque

Desde la raíz del repositorio:

```powershell
docker compose -f deploy/docker-compose.yml --profile demo up --build -d h2train-demo-api
```

Se indica el servicio `h2train-demo-api` expresamente para iniciar únicamente
la demo y su generador, sin levantar los demás servicios sin perfil.

La primera construcción puede tardar varios minutos. Después, la reconstrucción
de los eventos sintéticos tarda aproximadamente un minuto. El contenedor no se
marca como saludable hasta que el datamart completo alcanza la fecha final
`2026-06-01T23:59:00Z`.

Comprobar el estado:

```powershell
docker compose -f deploy/docker-compose.yml --profile demo ps h2train-demo-api
```

## Verificación automática

El siguiente script espera a que termine la reconstrucción y valida las cinco
consultas principales, una exportación CSV y el dataset de zonas cardiacas:

```powershell
.\deploy\demo\verify-demo-api.ps1
```

## Consultas demostrables

### Calorías medias superiores a 2500

```powershell
curl.exe "http://localhost:8082/api/v1/datasets/query?metric=daily_calories&operator=gt&value=2500&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Sujetos esperados:

```text
demo-u001, demo-u004, demo-u005, demo-u006, demo-u009, demo-u010
```

### Más de 10.000 pasos diarios de media

```powershell
curl.exe "http://localhost:8082/api/v1/datasets/query?metric=daily_steps&operator=gt&value=10000&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Sujetos esperados:

```text
demo-u002, demo-u005, demo-u006, demo-u008, demo-u010
```

### Menos de seis horas de sueño medio

La duración del sueño se almacena en segundos, por lo que seis horas equivalen
a `21600`.

```powershell
curl.exe "http://localhost:8082/api/v1/datasets/query?metric=daily_sleep_duration&operator=lt&value=21600&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Sujetos esperados:

```text
demo-u003, demo-u006, demo-u008
```

### Más de cuatro actividades semanales

```powershell
curl.exe "http://localhost:8082/api/v1/datasets/query?metric=weekly_activity_count&operator=gt&value=4&aggregation=avg&from=2026-01-01&to=2026-06-01&format=json"
```

Sujetos esperados:

```text
demo-u004, demo-u005, demo-u006, demo-u010
```

### Más de 30 km semanales corriendo

La distancia se almacena en metros y se filtra la dimensión
`activityType=run`.

```powershell
curl.exe "http://localhost:8082/api/v1/datasets/query?metric=weekly_workout_distance_by_sport&operator=gt&value=30000&aggregation=avg&activityType=run&from=2026-01-01&to=2026-06-01&format=json"
```

Sujetos esperados:

```text
demo-u005, demo-u006
```

## Exportación CSV

```powershell
$body = @{
    metrics = @(
        "daily_steps",
        "daily_calories",
        "daily_sleep_duration"
    )
    filters = @(
        @{
            metric = "daily_calories"
            operator = "gt"
            value = 2500
            aggregation = "avg"
        }
    )
    from = "2026-01-01"
    to = "2026-06-01"
    format = "csv"
} | ConvertTo-Json -Depth 6

Invoke-WebRequest `
    -Uri "http://localhost:8082/api/v1/datasets/export" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body `
    -OutFile "h2train-demo-dataset.csv"
```

## Regeneración

El generador es determinista e idempotente. Este comando reemplaza únicamente
los eventos y datamarts del volumen de demostración:

```powershell
docker compose -f deploy/docker-compose.yml --profile demo up --build -d --force-recreate h2train-demo-api
```

## Detención y eliminación

Detener y eliminar los contenedores de demostración:

```powershell
docker compose -f deploy/docker-compose.yml --profile demo rm -sf h2train-demo-api h2train-demo-data
```

Eliminar únicamente sus datos:

```powershell
docker volume rm h2train-data-demo-storage
```

No debe utilizarse `docker compose down -v` para limpiar la demo cuando el
despliegue real utiliza el mismo archivo Compose, porque ese comando también
podría eliminar otros volúmenes declarados por la plataforma.
