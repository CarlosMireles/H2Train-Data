# Despliegue Docker de H2Train-Data

Este directorio prepara H2Train-Data para un despliegue progresivo con Docker
Compose. No sustituye el desarrollo local con Maven y Spring Boot ni modifica
la lógica de negocio.

## Decisiones actuales y ventajas

### 1. Dockerfiles

Decisión: utilizar un único Dockerfile multietapa reutilizable en
`deploy/docker/Dockerfile.app`, parametrizado mediante `MODULE`.

Ventajas:

- Evita duplicar un Dockerfile por cada módulo Java.
- Mantiene todos los servicios alineados con la misma versión de Java y la
  misma estrategia de construcción.
- Compila únicamente el módulo Maven seleccionado y sus dependencias mediante
  `mvn -pl <module> -am`.
- Produce imágenes de ejecución sin Maven ni código fuente.
- Ejecuta los procesos con el usuario `h2train`, sin privilegios de root.

### 2. Persistencia

Decisión: mantener inicialmente la persistencia H2 basada en archivos.

Ventajas:

- Conserva el comportamiento actual de la aplicación.
- Evita añadir el controlador y las dependencias de PostgreSQL antes de que la
  aplicación esté preparada para esa migración.
- Mantiene la base de datos dentro del volumen compartido, bajo
  `/var/lib/h2train/database`.
- Reduce el riesgo mientras se conteneriza primero la infraestructura.

La migración futura a PostgreSQL sigue siendo posible, pero debe abordarse como
una tarea independiente porque modifica las condiciones de ejecución de la
base de datos.

### 3. Comprobaciones de estado

Decisión: añadir comprobaciones de estado de Docker en Compose.

Ventajas:

- Se comprueba que Kafka esté preparado antes de iniciar los servicios
  dependientes.
- Los servicios web se comprueban mediante endpoints HTTP.
- Los servicios no web se comprueban verificando que el proceso Java siga
  activo.
- No se modifica la lógica de negocio de la aplicación.

### 4. Perfiles de despliegue

Decisión: utilizar los perfiles de Docker Compose `local`, `staging`, `prod`,
`apps` y `tools`.

Ventajas:

- `local` puede incluir herramientas de desarrollo como Kafka UI.
- `staging` y `prod` pueden ejecutar los servicios sin Kafka UI de forma
  predeterminada.
- `apps` permite iniciar directamente todos los contenedores de H2Train.
- El mismo archivo de Compose puede evolucionar sin duplicar definiciones.

Los ejemplos de perfiles se encuentran en `deploy/profiles/`.

### 5. Registros

Decisión: conservar la salida estándar de Docker y configurar también los
registros de Spring Boot bajo `/var/lib/h2train/logs/<service>`.

Ventajas:

- Los registros de Docker siguen disponibles mediante `docker compose logs`.
- Los registros persistentes están disponibles en el volumen compartido
  `h2train-storage`.
- Cada servicio dispone de un directorio independiente.
- No son necesarios cambios de código porque Spring Boot interpreta
  `LOGGING_FILE_PATH` automáticamente.

## Estructura

```text
deploy/
├── docker-compose.yml
├── docker/
│   └── Dockerfile.app
├── env/
│   ├── common.env
│   ├── portal.env
│   ├── daemon.env
│   ├── datalake.env
│   └── data-api.env
├── env.example/
│   ├── common.env.example
│   ├── portal.env.example
│   ├── daemon.env.example
│   ├── datalake.env.example
│   └── data-api.env.example
└── profiles/
    ├── local.env.example
    ├── staging.env.example
    └── prod.env.example
```

## Almacenamiento de ejecución

Todos los contenedores de la aplicación comparten este volumen:

```text
/var/lib/h2train/
├── database/
├── datalake/
│   ├── events/
│   └── datamarts/
├── exports/
└── logs/
```

El volumen de Compose se llama `h2train-storage` y se monta en
`/var/lib/h2train`.

## Archivos de entorno

`deploy/env/` contiene los valores locales utilizados por Docker Compose. Estos
archivos pueden contener secretos y no deben incluirse en Git.

`deploy/env.example/` contiene ejemplos seguros que deben permanecer
versionados. Para preparar un entorno nuevo, se copian las plantillas desde
`env.example` a `env`.

### common.env

Utilizado por todos los servicios de H2Train.

Contiene los valores compartidos de infraestructura:

```text
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC=h2train.events.v1
H2TRAIN_STORAGE_ROOT=/var/lib/h2train
DATALAKE_ROOT_PATH=/var/lib/h2train/datalake
H2TRAIN_DB_URL=jdbc:h2:file:/var/lib/h2train/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false
H2TRAIN_DB_USERNAME=sa
H2TRAIN_DB_PASSWORD=
```

### portal.env

Utilizado por `h2train-portal`.

Completar manualmente:

```text
STRAVA_CLIENT_ID
STRAVA_CLIENT_SECRET
STRAVA_REDIRECT_URI
FITBIT_CLIENT_ID
FITBIT_CLIENT_SECRET
FITBIT_REDIRECT_URI
GOOGLE_LOGIN_ENABLED
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
```

Envío opcional de correo para recuperar contraseñas:

```text
PASSWORD_RESET_EMAIL_ENABLED
PASSWORD_RESET_EMAIL_FROM
PASSWORD_RESET_EMAIL_SUBJECT
SPRING_MAIL_HOST
SPRING_MAIL_PORT
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE
```

### daemon.env

Utilizado por `h2train-daemon`.

Contiene la configuración de sincronización y acceso a proveedores:

```text
APP_PERSISTENCE_TYPE
APP_BUS_TYPE
LOGGING_FILE_PATH
SYNC_POLL_INTERVAL_MS
SYNC_CONNECTION_PARALLELISM
SYNC_ACTIVITY_PARALLELISM
SYNC_METRICS_PARALLELISM
PROVIDER_HTTP_CONNECT_TIMEOUT
PROVIDER_HTTP_READ_TIMEOUT
STRAVA_CLIENT_ID
STRAVA_CLIENT_SECRET
STRAVA_REDIRECT_URI
FITBIT_ENABLED
FITBIT_CLIENT_ID
FITBIT_CLIENT_SECRET
FITBIT_REDIRECT_URI
FITBIT_INITIAL_ACTIVITY_FETCH_LIMIT
FITBIT_INCREMENTAL_ACTIVITY_FETCH_LIMIT
```

### datalake.env

Utilizado por `h2train-datalake`.

Contiene el destino de escritura y la identidad del consumidor Kafka:

```text
DATALAKE_ROOT_PATH
APP_BUS_CONSUMER_TYPE
LOGGING_FILE_PATH
DATALAKE_KAFKA_GROUP_ID
DATALAKE_KAFKA_CLIENT_ID
DATALAKE_KAFKA_AUTO_OFFSET_RESET
```

### data-api.env

Utilizado por `h2train-data-app`.

Contiene la ruta de lectura del datamart, el puerto de la API y la identidad
del consumidor de proyecciones temporales:

```text
DATALAKE_ROOT_PATH
LONGITUDINAL_DATAMART_PATH
SERVER_PORT
LOGGING_FILE_PATH
APP_DATA_APP_BUS_TYPE
TIMESERIES_KAFKA_GROUP_ID
TIMESERIES_KAFKA_CLIENT_ID
TIMESERIES_KAFKA_AUTO_OFFSET_RESET
TIMESERIES_REBUILD_ON_STARTUP
```

## Comandos

Validar la configuración de Compose:

```powershell
docker compose -f deploy/docker-compose.yml config
docker compose -f deploy/docker-compose.yml --profile apps config
```

Iniciar únicamente Kafka y Kafka UI para depuración local:

```powershell
docker compose -f deploy/docker-compose.yml --profile tools up kafka kafka-ui
```

Construir e iniciar todos los servicios para una ejecución Docker local:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up --build
```

Iniciar todos los servicios sin herramientas locales:

```powershell
docker compose -f deploy/docker-compose.yml --profile apps up --build
```

## Política de Git

No incluir:

```text
deploy/env/*.env
```

Mantener versionado:

```text
.dockerignore
deploy/docker-compose.yml
deploy/docker/Dockerfile.app
deploy/env.example/*.env.example
deploy/profiles/*.env.example
deploy/README.md
```

## Próximos pasos pendientes

1. Ejecutar una construcción Docker real cuando los secretos de proveedores
   estén configurados localmente.
2. Añadir gestión de secretos adecuada para producción, por ejemplo Docker
   secrets o un almacén externo.
3. Decidir e implementar soporte para PostgreSQL si H2 deja de ser suficiente
   para despliegues compartidos o productivos.
4. Añadir proxy inverso y TLS si los servicios se exponen fuera de localhost.
5. Añadir tareas de integración continua para construir y publicar las
   imágenes Docker.
