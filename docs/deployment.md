# Deployment

Docker support is prepared under:

```text
deploy/
  docker-compose.yml
  docker/
  env/
  env.example/
  profiles/
  README.md
```

## Compose services

The planned services are:

- `h2train-portal`
- `h2train-daemon`
- `h2train-datalake`
- `h2train-data-app`
- `kafka`
- `kafka-ui`

## Shared storage

All application containers mount the named Docker volume at:

```text
/var/lib/h2train
```

Expected structure:

```text
/var/lib/h2train/
  database/
  datalake/
    events/
    datamarts/
  exports/
  logs/
```

## Environment files

Real environment files live in `deploy/env/` and are ignored by Git. Sanitized templates live in `deploy/env.example/` and are versioned.

## Local Docker commands

From the repository root:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build
```

Rebuild after code changes:

```powershell
docker compose -f deploy/docker-compose.yml --profile local up -d --build --force-recreate
```

Stop services:

```powershell
docker compose -f deploy/docker-compose.yml --profile local down
```

## Notes

`deploy/docker-compose.yml` is the only Compose definition. Use `-f deploy/docker-compose.yml` when running commands from the repository root.
