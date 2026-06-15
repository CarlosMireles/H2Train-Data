# Configuración

La configuración se controla mediante variables de entorno y utiliza valores
predeterminados seguros para desarrollo local cuando es posible.

## Variables compartidas

| Variable | Propósito | Valor habitual en Docker |
| --- | --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | Dirección del broker de Kafka. | `kafka:9092` |
| `KAFKA_TOPIC` | Tema principal de eventos normalizados. | `h2train.events.v1` |
| `H2TRAIN_STORAGE_ROOT` | Raíz de almacenamiento compartido. | `/var/lib/h2train` |
| `DATALAKE_ROOT_PATH` | Ruta raíz del datalake. | `/var/lib/h2train/datalake` |

Los valores locales predeterminados se resuelven desde la raíz del repositorio:

```text
H2TRAIN_DB_URL=jdbc:h2:file:./runtime/local/database/h2train;...
DATALAKE_ROOT_PATH=./runtime/local/datalake
```

## Variables del portal

| Variable | Propósito |
| --- | --- |
| `APP_PERSISTENCE_TYPE` | Implementación de persistencia, normalmente `jdbc`. |
| `APP_BUS_TYPE` | Publicador del bus de eventos, por ejemplo `kafka` o `logging`. |
| `H2TRAIN_DB_URL` | URL JDBC de la base de datos del portal y del daemon. |
| `STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_REDIRECT_URI` | Configuración OAuth de Strava. |
| `FITBIT_ENABLED`, `FITBIT_CLIENT_ID`, `FITBIT_CLIENT_SECRET`, `FITBIT_REDIRECT_URI` | Configuración OAuth de Fitbit. |
| `GOOGLE_LOGIN_ENABLED`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` | Configuración de acceso con Google. |
| `PASSWORD_RESET_EMAIL_ENABLED` | Activa el envío SMTP para recuperar contraseñas. |
| `PASSWORD_RESET_EMAIL_FROM` | Dirección remitente de los correos de recuperación. |
| `SPRING_MAIL_*` | Configuración SMTP utilizada por Spring Mail. |

## Variables del daemon

El daemon necesita acceso a la base de datos, configuración OAuth de los
proveedores, parámetros de sincronización y configuración del bus de eventos.
No debe contener variables exclusivas de la interfaz del portal.

## Variables del datalake

El escritor del datalake solo necesita la ruta raíz y la configuración del
consumidor Kafka:

- `DATALAKE_ROOT_PATH`
- `APP_BUS_CONSUMER_TYPE`
- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_TOPIC`
- `DATALAKE_KAFKA_GROUP_ID`
- `DATALAKE_KAFKA_CLIENT_ID`

## Variables de la API de datos

La API de datos solo necesita el almacenamiento del modelo de lectura y su
puerto de ejecución:

- `DATALAKE_ROOT_PATH`
- `LONGITUDINAL_DATAMART_PATH`
- `SERVER_PORT`
- variables del consumidor de proyección cuando la proyección temporal se
  ejecuta en este módulo

## Política de secretos

Los secretos reales no deben incluirse en Git. Utilizar:

```text
deploy/env/*.env
```

para los valores locales o de despliegue y conservar únicamente ejemplos
anonimizados en:

```text
deploy/env.example/*.env.example
```
