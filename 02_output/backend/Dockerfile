# Build stage: JDK image of the pinned Temurin release (build only, D-30).
FROM eclipse-temurin:21.0.10_7-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/main/ src/main/
RUN ./mvnw -B -q -o package -DskipTests

# Runtime stage: JRE image, non-root user (SB-11).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build /build/target/registration-backend-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
