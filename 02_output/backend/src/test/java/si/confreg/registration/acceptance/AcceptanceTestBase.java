package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Black-box harness: the backend runs on a random port against PostgreSQL and Mailpit containers
 * (images pinned in tech-stack.md) and is driven only over HTTP; stored rows are read only through
 * the storage contract (docs/02_contracts/registration-storage.sql).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(AcIdDisplayNames.class)
@TestPropertySource(locations = "classpath:acceptance/acceptance.properties")
abstract class AcceptanceTestBase {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025);

  static final String ORGANIZER_USERNAME = "organizer-" + UUID.randomUUID();
  static final String ORGANIZER_PASSWORD = UUID.randomUUID().toString();

  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  @LocalServerPort int port;

  @Autowired Environment environment;

  @DynamicPropertySource
  static void containerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> mailPort());
    registry.add("organizer.username", () -> ORGANIZER_USERNAME);
    registry.add("organizer.password", () -> ORGANIZER_PASSWORD);
  }

  /** SMTP port the backend sends to; overridden by tests that need a failing SMTP server. */
  static int mailPort() {
    return MAILPIT.getMappedPort(1025);
  }

  @BeforeEach
  void checkContainers() {
    assertThat(POSTGRES.isRunning()).isTrue();
    assertThat(MAILPIT.isRunning()).isTrue();
  }

  // ---- configuration (AR-04: expected values come from configuration) ----

  BigDecimal feeEarly() {
    return new BigDecimal(environment.getRequiredProperty("app.fee-early"));
  }

  BigDecimal feeRegular() {
    return new BigDecimal(environment.getRequiredProperty("app.fee-regular"));
  }

  BigDecimal vatRate() {
    return new BigDecimal(environment.getRequiredProperty("app.vat-rate"));
  }

  ZoneId conferenceZone() {
    return ZoneId.of(environment.getRequiredProperty("app.conference-tz"));
  }

  /** First instant after the early-bird deadline day, in the conference time zone. */
  Instant deadlineEnd() {
    LocalDate deadline =
        LocalDate.parse(environment.getRequiredProperty("app.early-bird-deadline"));
    return deadline.plusDays(1).atStartOfDay(conferenceZone()).toInstant();
  }

  Instant lastEarlyInstant() {
    return deadlineEnd().minusMillis(1);
  }

  Instant firstRegularInstant() {
    return deadlineEnd();
  }

  /** Configured workshop ids, in configuration order. */
  List<String> workshopIds() {
    List<String> ids = new ArrayList<>();
    for (String entry : environment.getRequiredProperty("app.workshops").split(";")) {
      ids.add(entry.substring(0, entry.indexOf('=')).trim());
    }
    return ids;
  }

  static BigDecimal expectedNet(BigDecimal gross, BigDecimal rate) {
    return gross.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
  }

  // ---- requests ----

  static String uniqueEmail() {
    return "p-" + UUID.randomUUID() + "@example.com";
  }

  static ObjectNode privateRegistration() {
    ObjectNode body = JSON.createObjectNode();
    body.put("firstName", "Ana");
    body.put("lastName", "Kovač");
    body.put("email", uniqueEmail());
    body.put("payerType", "private");
    body.putArray("workshops");
    return body;
  }

  static ObjectNode companyRegistration() {
    ObjectNode body = JSON.createObjectNode();
    body.put("firstName", "Žiga");
    body.put("lastName", "Šuštar");
    body.put("email", uniqueEmail());
    body.put("payerType", "company");
    body.put("companyName", "Primer d.o.o.");
    body.put("companyAddress", "Čopova ulica 1, 1000 Ljubljana");
    body.put("companyVatId", "SI12345678");
    body.putArray("workshops");
    return body;
  }

  HttpResponse<String> register(JsonNode body, Instant now) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri("/api/registrations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    if (now != null) {
      request.header("X-Test-Now", now.toString());
    }
    return send(request.build());
  }

  HttpResponse<String> register(JsonNode body) {
    return register(body, null);
  }

  HttpResponse<String> getAsOrganizer(String registrationNumber) {
    String credentials = ORGANIZER_USERNAME + ":" + ORGANIZER_PASSWORD;
    String header =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    return send(
        HttpRequest.newBuilder(uri("/api/registrations/" + registrationNumber))
            .header("Authorization", header)
            .GET()
            .build());
  }

  HttpResponse<String> getWithoutCredentials(String registrationNumber) {
    return send(
        HttpRequest.newBuilder(uri("/api/registrations/" + registrationNumber)).GET().build());
  }

  URI uri(String path) {
    return URI.create("http://127.0.0.1:" + port + path);
  }

  static HttpResponse<String> send(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("HTTP request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request interrupted", e);
    }
  }

  static JsonNode json(HttpResponse<String> response) {
    return JSON.readTree(response.body());
  }

  /** Reads an amount that the API may return as a JSON number or a decimal string. */
  static BigDecimal amount(JsonNode registration, String field) {
    JsonNode node = registration.get(field);
    assertThat(node).as(field).isNotNull();
    return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asString());
  }

  /** Asserts a completed registration (2xx) and returns the stored registration. */
  static JsonNode completed(HttpResponse<String> response) {
    assertThat(response.statusCode()).as("status, body: %s", response.body()).isBetween(200, 299);
    return json(response);
  }

  // ---- storage contract ----

  static long storedRegistrations() {
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT count(*) FROM registration")) {
      result.next();
      return result.getLong(1);
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot read the registration table", e);
    }
  }

  // ---- mail (Mailpit HTTP API) ----

  /** Plain-text bodies and subjects of every message sent to the address, keyed by Mailpit id. */
  static Map<String, JsonNode> messagesTo(String address) {
    URI search =
        URI.create(
            "http://"
                + MAILPIT.getHost()
                + ":"
                + MAILPIT.getMappedPort(8025)
                + "/api/v1/search?query="
                + java.net.URLEncoder.encode("to:\"" + address + "\"", StandardCharsets.UTF_8));
    JsonNode list = json(send(HttpRequest.newBuilder(search).GET().build()));
    Map<String, JsonNode> messages = new LinkedHashMap<>();
    for (JsonNode summary : list.path("messages")) {
      if (!isAddressedTo(summary, address)) {
        continue; // Mailpit's "to:" search matches substrings; keep exact recipients only (D-33)
      }
      String id = summary.get("ID").asString();
      URI message =
          URI.create(
              "http://"
                  + MAILPIT.getHost()
                  + ":"
                  + MAILPIT.getMappedPort(8025)
                  + "/api/v1/message/"
                  + id);
      messages.put(id, json(send(HttpRequest.newBuilder(message).GET().build())));
    }
    return messages;
  }

  private static boolean isAddressedTo(JsonNode summary, String address) {
    for (JsonNode recipient : summary.path("To")) {
      if (recipient.path("Address").asString("").equalsIgnoreCase(address)) {
        return true;
      }
    }
    return false;
  }

  /** Waits until Mailpit has stored the expected number of messages for the address. */
  static Map<String, JsonNode> awaitMessagesTo(String address, int expected) {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    Map<String, JsonNode> messages = messagesTo(address);
    while (messages.size() < expected && System.nanoTime() < deadline) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      messages = messagesTo(address);
    }
    return messages;
  }

  /** Asserts the request was rejected (4xx) without storing a registration or sending e-mail. */
  void assertRejected(JsonNode body) {
    long before = storedRegistrations();
    HttpResponse<String> response = register(body);
    assertThat(response.statusCode()).as("status, body: %s", response.body()).isBetween(400, 499);
    assertThat(storedRegistrations()).as("stored registrations").isEqualTo(before);
    JsonNode email = body.get("email");
    if (email != null && email.isString() && email.asString().contains("@")) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      assertThat(messagesTo(email.asString().trim())).as("e-mails sent").isEmpty();
    }
  }
}
