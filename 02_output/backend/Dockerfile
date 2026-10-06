# Backend image (spec 10.1). Build the jar first: ./mvnw -DskipTests package
# (no JDK image is listed in tech-stack.md; the jar is built on the host, D-01).
FROM eclipse-temurin:21.0.10_7-jre-alpine

RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app
COPY --chown=root:root target/registration-backend-0.1.0.jar /app/app.jar

# Non-root (SB-11). API on 8080; health and readiness on 8081, container loopback only (D-15).
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
