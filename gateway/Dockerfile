# =====================================================================
# Gateway — build multi-stage (contexte = racine du projet multi-module)
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Cache des dépendances : on copie d'abord les POMs.
COPY pom.xml .
COPY gateway/pom.xml gateway/
COPY tender-service/pom.xml tender-service/
COPY user-service/pom.xml user-service/
RUN mvn -q -pl gateway -am dependency:go-offline -DskipTests || true
# Puis le code du module ciblé.
COPY gateway/src gateway/src
RUN mvn -q -pl gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
# Utilisateur non-root (durcissement — Annexe A / 4.6 Security Misconfiguration)
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /workspace/gateway/target/gateway.jar app.jar
USER app
EXPOSE 8080 8443
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]