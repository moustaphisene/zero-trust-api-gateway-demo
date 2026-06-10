# =====================================================================
# user-service — build multi-stage (contexte = racine du projet)
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY gateway/pom.xml gateway/
COPY tender-service/pom.xml tender-service/
COPY user-service/pom.xml user-service/
RUN mvn -q -pl user-service -am dependency:go-offline -DskipTests || true
COPY user-service/src user-service/src
RUN mvn -q -pl user-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]