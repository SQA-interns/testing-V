# Build stage: pinned Temurin JDK image of the same version as the runtime JRE (D-17).
FROM eclipse-temurin:21.0.10_7-jdk-alpine AS build
WORKDIR /build
COPY mvnw pom.xml spotbugs-exclude.xml dependency-check-suppressions.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/main src/main
RUN ./mvnw -B -q package -DskipTests && cp target/registration-backend-*.jar /build/app.jar

# Runtime stage: JRE only, non-root user (SB-11).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /build/app.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
