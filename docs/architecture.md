# Arquitectura

H2Train-Data está organizado como una plataforma modular de ingesta de datos
procedentes de proveedores deportivos y de salud.

## Flujo de datos

```text
Portal web
    -> Base de datos del portal
    -> Daemon de sincronización
    -> Bus Kafka
    -> Datalake de eventos
    -> Datamarts longitudinales
    -> API de datos de solo lectura
```

## Reglas arquitectónicas

- `events/` es la fuente de verdad para los datos de proveedores ya
  normalizados como eventos.
- `datamarts/longitudinal/` es un modelo de lectura materializado derivado de
  los eventos.
- Kafka distribuye eventos entre consumidores independientes.
- El escritor del datalake persiste registros de eventos inmutables.
- La proyección de series temporales actualiza los datamarts longitudinales de
  forma incremental.
- La API de datos solo consulta `datamarts/longitudinal/` y no debe acceder
  directamente a `events/`.
- El reconstructor es una herramienta operativa de recuperación que puede
  regenerar los datamarts desde los eventos, pero no forma parte del flujo
  normal de ejecución.

## Separación CQRS

Modelo de escritura:

```text
runtime/local/datalake/events/{eventType}/{YYYY-MM-DD}.jsonl
```

Modelo de lectura:

```text
runtime/local/datalake/datamarts/longitudinal/
```

Esta separación mantiene la ingesta orientada a anexar eventos y permite que
las consultas de la API sean rápidas y estables para paneles, exportaciones de
investigación y análisis longitudinales.

## Almacenamiento de ejecución

El desarrollo local utiliza una única raíz para los datos generados:

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

El despliegue en contenedores utiliza la raíz de almacenamiento compartido:

```text
/var/lib/h2train/
  database/
  datalake/
    events/
    datamarts/
  exports/
  logs/
```

Git ignora deliberadamente los datos generados durante la ejecución.
