# syntax=docker/dockerfile:1.7

ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace

COPY pom.xml ./
COPY h2train-bus/pom.xml h2train-bus/pom.xml
COPY h2train-provider-sync/pom.xml h2train-provider-sync/pom.xml
COPY h2train-daemon/pom.xml h2train-daemon/pom.xml
COPY h2train-datalake/pom.xml h2train-datalake/pom.xml
COPY h2train-data-app/pom.xml h2train-data-app/pom.xml
COPY h2train-portal/pom.xml h2train-portal/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline

COPY h2train-bus h2train-bus
COPY h2train-provider-sync h2train-provider-sync
COPY h2train-daemon h2train-daemon
COPY h2train-datalake h2train-datalake
COPY h2train-data-app h2train-data-app
COPY h2train-portal h2train-portal

ARG MODULE
RUN --mount=type=cache,target=/root/.m2 test -n "${MODULE}" \
    && mvn -B -pl "${MODULE}" -am -DskipTests package \
    && cp "${MODULE}"/target/"${MODULE}"-*.jar /tmp/h2train-app.jar

FROM ${RUNTIME_IMAGE} AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system h2train \
    && useradd --system --gid h2train --home-dir /app --shell /usr/sbin/nologin h2train \
    && mkdir -p /app /var/lib/h2train/database /var/lib/h2train/datalake/events /var/lib/h2train/datalake/datamarts /var/lib/h2train/exports /var/lib/h2train/logs \
    && chown -R h2train:h2train /app /var/lib/h2train

WORKDIR /app
COPY --from=build --chown=h2train:h2train /tmp/h2train-app.jar /app/app.jar

ENV JAVA_OPTS="" \
    H2TRAIN_STORAGE_ROOT=/var/lib/h2train

USER h2train
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
