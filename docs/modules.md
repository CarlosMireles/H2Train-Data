# Modules

The repository is a Maven multi-module project targeting Java 17 and Spring Boot 3.3.4.

## Root modules

| Module | Responsibility |
| --- | --- |
| `h2train-bus` | Shared event envelope, bus abstractions, Kafka publisher support and privacy metadata. |
| `h2train-provider-sync` | Provider integration domain, OAuth connectors, sync use cases and provider persistence ports. |
| `h2train-daemon` | Background scheduler that synchronizes due provider connections and publishes normalized events. |
| `h2train-datalake` | Kafka consumer that writes normalized events to the event datalake. |
| `h2train-data-app` | Read-only REST API over longitudinal datamarts and time-series projections. |
| `h2train-portal` | Spring Boot MVC portal, internal accounts, provider configuration, OAuth flows and rendered HTML UI. |

## Non-code directories

| Directory | Responsibility |
| --- | --- |
| `deploy/` | Docker Compose scaffold, Dockerfile, environment examples and deployment notes. |
| `docs/` | Project documentation split by topic. |
| `runtime/local/database/` | Local generated H2 database files, ignored by Git. |
| `runtime/local/datalake/` | Local generated event and datamart files, ignored by Git. |
| `runtime/local/backups/` | Local migration backups, ignored by Git. |

## Package organization

The current Java package layout already separates application services, ports, infrastructure, configuration, web controllers and domain models. Large package moves are intentionally avoided because they do not improve runtime behavior and increase regression risk.
