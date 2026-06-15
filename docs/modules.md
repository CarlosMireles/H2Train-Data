# Módulos

El repositorio es un proyecto Maven multimódulo basado en Java 17 y Spring Boot
3.3.4.

## Módulos raíz

| Módulo | Responsabilidad |
| --- | --- |
| `h2train-bus` | Envoltorio compartido de eventos, abstracciones del bus, publicación en Kafka y metadatos de privacidad. |
| `h2train-provider-sync` | Dominio de integración, conectores OAuth, casos de uso de sincronización y puertos de persistencia de proveedores. |
| `h2train-daemon` | Planificador en segundo plano que sincroniza las conexiones pendientes y publica eventos normalizados. |
| `h2train-datalake` | Consumidor Kafka que escribe eventos normalizados en el datalake. |
| `h2train-data-app` | API REST de solo lectura sobre datamarts longitudinales y proyecciones temporales. |
| `h2train-portal` | Portal Spring Boot MVC, cuentas internas, configuración de proveedores, flujos OAuth e interfaz HTML. |

## Directorios sin código

| Directorio | Responsabilidad |
| --- | --- |
| `deploy/` | Docker Compose, Dockerfile, ejemplos de entorno y notas de despliegue. |
| `docs/` | Documentación del proyecto separada por temas. |
| `runtime/local/database/` | Archivos locales generados de H2, ignorados por Git. |
| `runtime/local/datalake/` | Eventos y datamarts locales generados, ignorados por Git. |
| `runtime/local/backups/` | Copias de seguridad locales de migraciones, ignoradas por Git. |

## Organización de paquetes

La estructura actual de paquetes Java ya separa servicios de aplicación,
puertos, infraestructura, configuración, controladores web y modelos de
dominio. Se evitan movimientos masivos de paquetes porque no mejoran el
comportamiento de ejecución y aumentan el riesgo de regresiones.
