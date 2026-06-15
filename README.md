# H2Train-Data

H2Train-Data es una plataforma modular basada en Spring Boot para ingerir datos
de proveedores deportivos y de salud, almacenar eventos normalizados, construir
datamarts longitudinales y exponer API de datos de solo lectura.

## Arquitectura

```text
Portal web
    -> Base de datos del portal
    -> Daemon de sincronización
    -> Bus Kafka
    -> Datalake de eventos
    -> Datamarts longitudinales
    -> API de datos de solo lectura
```

Reglas principales:

- `events/` es la fuente de verdad.
- `datamarts/longitudinal/` es un modelo de lectura materializado.
- Kafka desacopla productores y consumidores.
- La API solo consulta datamarts, nunca los archivos de eventos directamente.
- Git ignora los datos generados durante la ejecución.

## Módulos

| Módulo | Responsabilidad |
| --- | --- |
| `h2train-bus` | Abstracciones compartidas del bus de eventos y soporte para Kafka. |
| `h2train-provider-sync` | OAuth, clientes de proveedores y casos de uso de sincronización. |
| `h2train-daemon` | Planificador de sincronización en segundo plano. |
| `h2train-datalake` | Consumidor Kafka que escribe eventos normalizados en el datalake. |
| `h2train-data-app` | API REST sobre los datamarts longitudinales. |
| `h2train-portal` | Portal Spring Boot MVC para cuentas, proveedores y configuración. |
| `deploy` | Definición Docker Compose y plantillas de entorno. |
| `docs` | Documentación ampliada del proyecto. |

## Documentación

- [Arquitectura](docs/architecture.md)
- [Módulos](docs/modules.md)
- [Configuración](docs/configuration.md)
- [API](docs/api.md)
- [API de datasets personalizados](docs/datasets-api.md)
- [Datalake](docs/datalake.md)
- [Datamarts](docs/datamarts.md)
- [Despliegue](docs/deployment.md)
- [Desarrollo](docs/development.md)

Toda la documentación explicativa se mantiene en español. Los identificadores
técnicos, nombres de métricas, eventos, parámetros y variables conservan su
forma original porque pertenecen a los contratos de la plataforma.

## Desarrollo local

Requisitos:

- Java 17
- Maven
- Kafka para ejecutar la ingesta y las proyecciones respaldadas por Kafka
- Credenciales de proveedores para probar flujos reales de Strava, Fitbit o Google

Compilar todos los módulos:

```powershell
mvn clean compile
```

Ejecutar la verificación completa:

```powershell
mvn clean verify
```

Ejecutar el portal localmente:

```powershell
. .\scripts\load-local-env.ps1 portal
mvn -pl h2train-portal -am spring-boot:run
```

Ejecutar la API de datos localmente:

```powershell
. .\scripts\load-local-env.ps1 data-api
mvn -pl h2train-data-app -am spring-boot:run
```

Consultar [Desarrollo](docs/development.md) para ver el flujo completo desde
terminales de IntelliJ.

## Docker

Los archivos de despliegue Docker se encuentran en `deploy/`.

Iniciar el perfil local:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build
```

Reconstruir los contenedores después de cambios en el código:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build --force-recreate
```

La ruta de almacenamiento compartido de Docker es:

```text
/var/lib/h2train
```

## Datos de ejecución

Los datos locales generados no deben incluirse en Git:

```text
runtime/
  local/
    database/
    datalake/
      events/
      datamarts/
    backups/
    exports/
    logs/
```

En Docker, estos mismos datos se almacenan bajo `/var/lib/h2train/`.

## Archivos de entorno

Los archivos de entorno reales están ignorados:

```text
deploy/env/*.env
.env
.env.*
```

Los ejemplos versionados se encuentran en:

```text
deploy/env.example/*.env.example
```

No se deben incluir en Git secretos OAuth, contraseñas SMTP, tokens de
renovación, archivos de base de datos H2 ni contenido generado del datalake.
