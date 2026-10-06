# Backend runtime image (SB-11: non-root). Build the jar first: ./mvnw -B package -DskipTests
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
COPY target/registration-backend-0.1.0.jar /app/app.jar
USER 10001
# 8080 API; 8081 health and readiness, internal only (D-26)
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
