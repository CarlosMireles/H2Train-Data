# Desarrollo

## Requisitos

- Java 17
- Wrapper de Maven o instalación local de Maven
- Docker Desktop para la ejecución en contenedores
- Credenciales OAuth de los proveedores para probar flujos reales

## Compilación local con Maven

Compilar todos los módulos:

```powershell
mvn clean compile
```

Ejecutar la verificación completa:

```powershell
mvn clean verify
```

## Ejecución local de módulos

Este flujo híbrido ejecuta Kafka en Docker y los módulos Spring Boot en
terminales independientes de IntelliJ.

Iniciar Kafka desde la raíz del repositorio:

```powershell
docker compose -f deploy/docker-compose.yml up -d kafka
```

Cada terminal debe cargar su propio entorno porque las variables de PowerShell
son locales al proceso.

Terminal del portal:

```powershell
. .\scripts\load-local-env.ps1 portal
mvn -pl h2train-portal -am spring-boot:run
```

Terminal del daemon:

```powershell
. .\scripts\load-local-env.ps1 daemon
mvn -pl h2train-daemon -am spring-boot:run
```

Terminal del consumidor del datalake:

```powershell
. .\scripts\load-local-env.ps1 datalake
mvn -pl h2train-datalake -am spring-boot:run
```

Terminal de la API de datos:

```powershell
. .\scripts\load-local-env.ps1 data-api
mvn -pl h2train-data-app -am spring-boot:run
```

Para ejecutar el portal o el daemon sin repositorios persistentes:

```powershell
. .\scripts\load-local-env.ps1 portal -Persistence memory
```

El cargador lee las credenciales y la configuración desde `deploy/env/*.env`
y después aplica las direcciones y rutas locales. No muestra secretos.

Inspeccionar eventos de Kafka:

```powershell
docker compose -f deploy/docker-compose.yml exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic h2train.events.v1 --from-beginning
```

Inspeccionar conexiones sincronizadas de proveedores:

```powershell
java -cp "$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./runtime/local/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false" -user sa -sql "SELECT provider_id, athlete_id, sync_enabled, sync_interval, last_synced_at, user_id FROM provider_connections"
```

Inspeccionar usuarios:

```powershell
java -cp "$env:USERPROFILE\.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./runtime/local/database/h2train;AUTO_SERVER=TRUE;MODE=PostgreSQL;DATABASE_TO_UPPER=false" -user sa -sql "SELECT id, email, username, provider_ids, created_at FROM user_accounts"
```

## Datos de ejecución

Los datos generados deben permanecer fuera de los archivos versionados. Las
carpetas locales habituales son:

```text
runtime/local/
  database/
  datalake/
    events/
    datamarts/
  backups/
  exports/
  logs/
```

Git ignora `runtime/`. Las configuraciones de Maven utilizan la raíz del
repositorio como directorio de trabajo para que todos los módulos resuelvan las
mismas rutas. Las configuraciones de ejecución de IntelliJ deben hacer lo
mismo.

## Reglas de desarrollo

- Mantener la lógica de negocio en servicios o casos de uso, no en
  controladores.
- Mantener los eventos de proveedores como modelo de escritura.
- Mantener los datamarts como modelos de lectura derivados.
- No incluir secretos ni bases de datos H2 generadas en Git.
- Preferir refactorizaciones pequeñas que conserven el comportamiento frente a
  movimientos globales de paquetes.
