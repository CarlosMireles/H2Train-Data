# Despliegue

El soporte Docker se encuentra en:

```text
deploy/
  docker-compose.yml
  docker/
  env/
  env.example/
  profiles/
  README.md
```

## Servicios de Compose

Los servicios definidos son:

- `h2train-portal`
- `h2train-daemon`
- `h2train-datalake`
- `h2train-data-app`
- `kafka`
- `kafka-ui`

## Almacenamiento compartido

Todos los contenedores de la aplicación montan el volumen Docker en:

```text
/var/lib/h2train
```

Estructura esperada:

```text
/var/lib/h2train/
  database/
  datalake/
    events/
    datamarts/
  exports/
  logs/
```

## Archivos de entorno

Los archivos de entorno reales se encuentran en `deploy/env/` y Git los
ignora. Las plantillas anonimizadas se encuentran en `deploy/env.example/` y
sí están versionadas.

## Comandos Docker locales

Desde la raíz del repositorio:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build
```

Reconstruir después de cambios en el código:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build --force-recreate
```

Detener los servicios:

```powershell
docker compose -f deploy/docker-compose.yml --profile local down
```

## Notas

`deploy/docker-compose.yml` es la única definición de Compose. Al ejecutar
comandos desde la raíz del repositorio se debe indicar
`-f deploy/docker-compose.yml`.
