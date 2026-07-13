# Multi-stage build: (1) compile from source with Maven, (2) extract Spring
# Boot's layered jar into separate directories, (3) assemble a minimal
# runtime image from just those layers. Stage 1's output (a full JDK +
# Maven image) never ships — only the JRE-based final stage does.
#
# Maven's reactor needs every module's pom.xml present to resolve `-pl
# discovery-server` (it validates the full <modules> list in the root POM
# even when only building one module), so every service's pom.xml is
# copied here — but only discovery-server's actual source is, keeping this
# layer cache-friendly: the POM layer changes rarely, so a source change in
# one service doesn't invalidate every other service's Docker build cache.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY flight-service/pom.xml flight-service/pom.xml
COPY hotel-service/pom.xml hotel-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY loyalty-service/pom.xml loyalty-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -q -pl discovery-server dependency:go-offline
COPY discovery-server/src discovery-server/src
RUN mvn -q -pl discovery-server package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS extractor
WORKDIR /builder
COPY --from=build /workspace/discovery-server/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /application
COPY --from=extractor /builder/extracted/dependencies/ ./
COPY --from=extractor /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor /builder/extracted/application/ ./
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "application.jar"]
