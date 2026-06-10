# =====================================================================
# tender-service — build multi-stage (contexte = racine du projet)
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY gateway/pom.xml gateway/
COPY tender-service/pom.xml tender-service/
COPY user-service/pom.xml user-service/
RUN mvn -q -pl tender-service -am dependency:go-offline -DskipTests || true
COPY tender-service/src tender-service/src
RUN mvn -q -pl tender-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /workspace/tender-service/target/tender-service.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]