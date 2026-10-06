package si.confreg.registration.integration;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Backend on a random port against PostgreSQL and Mailpit containers (integration level). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:integration/integration.properties")
abstract class IntegrationTestBase {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025);

  static final String USERNAME = "it-" + UUID.randomUUID();
  static final String PASSWORD = UUID.randomUUID().toString();
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  @LocalServerPort int port;

  @DynamicPropertySource
  static void containers(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("organizer.username", () -> USERNAME);
    registry.add("organizer.password", () -> PASSWORD);
  }

  URI uri(String path) {
    return URI.create("http://127.0.0.1:" + port + path);
  }

  HttpResponse<String> post(String path, String contentType, String body) {
    return send(
        HttpRequest.newBuilder(uri(path))
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build());
  }

  HttpResponse<String> postJson(String body) {
    return post("/api/registrations", "application/json", body);
  }

  HttpResponse<String> get(String path, String username, String password) {
    HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
    if (username != null) {
      String token = username + ":" + password;
      request.header(
          "Authorization",
          "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
    }
    return send(request.build());
  }

  static HttpResponse<String> send(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  static JsonNode json(HttpResponse<String> response) {
    return JSON.readTree(response.body());
  }

  /** Plain text of the single message to the address, waiting up to 10 seconds. */
  static String mailTextTo(String address) throws InterruptedException {
    String base = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
    String query = URLEncoder.encode("to:\"" + address + "\"", StandardCharsets.UTF_8);
    for (int attempt = 0; attempt < 100; attempt++) {
      JsonNode list =
          json(
              send(
                  HttpRequest.newBuilder(URI.create(base + "/api/v1/search?query=" + query))
                      .build()));
      for (JsonNode message : list.path("messages")) {
        if (message.path("To").path(0).path("Address").asString("").equalsIgnoreCase(address)) {
          String id = message.get("ID").asString();
          return json(send(
                  HttpRequest.newBuilder(URI.create(base + "/api/v1/message/" + id)).build()))
              .get("Text")
              .asString();
        }
      }
      Thread.sleep(100);
    }
    throw new AssertionError("No e-mail to the address");
  }
}
