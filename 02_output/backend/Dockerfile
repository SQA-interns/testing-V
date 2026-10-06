# Runtime image only: build the jar first with ./mvnw -B package -DskipTests (README).
FROM eclipse-temurin:21.0.10_7-jre-alpine
RUN addgroup -S app && adduser -S -D -H -G app app
WORKDIR /app
COPY --chown=root:root --chmod=444 target/registration-backend-0.1.0.jar /app/app.jar
USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
