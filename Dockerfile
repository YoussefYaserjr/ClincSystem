# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Build the application
COPY src ./src
RUN mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system --gid 1001 app && \
    adduser --system --uid 1001 --ingroup app app

# curl is used by the container healthcheck (/actuator/health)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/clinicsystem-0.0.1-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

# Secrets (DB_PASSWORD, JWT_SECRET) must be provided at runtime via environment
ENV DB_URL=jdbc:mysql://localhost:3306/clinic?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true \
    DB_USERNAME=root

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
