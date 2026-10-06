package si.confreg.registration.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Running backend with PostgreSQL and Mailpit containers for integration tests (phase 5). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"app.test-clock=enabled", "app.rate-limit-per-hour=1000"})
abstract class IntegrationTestBase {

  static final String ORGANIZER = "organizer-integration";
  static final String PASSWORD = "integration-" + System.nanoTime();

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15-alpine");

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1").withExposedPorts(1025, 8025);

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  static final HttpClient HTTP = HttpClient.newHttpClient();

  @LocalServerPort int port;

  @DynamicPropertySource
  static void containers(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("app.organizer.username", () -> ORGANIZER);
    registry.add("app.organizer.password", () -> PASSWORD);
  }

  HttpResponse<String> send(HttpRequest request) throws Exception {
    return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
  }

  HttpResponse<String> post(String json) throws Exception {
    return send(
        request("/api/registrations")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build());
  }

  HttpResponse<String> get(String path, String user, String password) throws Exception {
    HttpRequest.Builder builder = request(path).GET();
    if (user != null) {
      String token =
          Base64.getEncoder()
              .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
      builder.header("Authorization", "Basic " + token);
    }
    return send(builder.build());
  }

  static String mailpit(String path) {
    return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path;
  }
}
