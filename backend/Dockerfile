# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system nutricoach && useradd --system --gid nutricoach --home /app nutricoach
WORKDIR /app
COPY --from=build /workspace/target/nutricoach-api-*.jar /app/nutricoach-api.jar
COPY --chown=nutricoach:nutricoach docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh
USER nutricoach
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
