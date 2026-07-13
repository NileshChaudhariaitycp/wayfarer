# See discovery-server/Dockerfile for the full explanation of this pattern.

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
RUN mvn -q -pl api-gateway dependency:go-offline
COPY api-gateway/src api-gateway/src
RUN mvn -q -pl api-gateway package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS extractor
WORKDIR /builder
COPY --from=build /workspace/api-gateway/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /application
COPY --from=extractor /builder/extracted/dependencies/ ./
COPY --from=extractor /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor /builder/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
