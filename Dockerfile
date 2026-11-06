# Family Hub: one image that serves the REST API and the built React app.
# Stage 1 builds frontend/ with Node, stage 2 builds the Spring Boot jar with the SPA inside static/,
# stage 3 is a small JRE image that runs it.
#
#   docker build -t family-hub .
#   docker run -p 8080:8080 --env-file .env -e SPRING_PROFILES_ACTIVE=prod family-hub
#
# Tests are not run here (they need Docker themselves for the Postgres check): run them before pushing.

# ---- 1. Frontend ----
FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. Backend ----
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
COPY --from=frontend /build/frontend/dist src/main/resources/static
RUN ./mvnw -B -q -DskipTests package && cp target/family-hub-*.jar app.jar

# ---- 3. Runtime ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /build/app.jar app.jar
USER app

# Household timezone (the app also uses it explicitly via app.timezone; this keeps logs in local time).
ENV TZ=Europe/Nicosia \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Render sets PORT; locally it defaults to 8080 (server.port=${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
