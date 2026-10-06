package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box harness for the US-001 acceptance tests: the backend runs on a random port with the
 * test clock enabled, against PostgreSQL and Mailpit containers. Tests use only the registration
 * API (contract {@code registration-api.openapi.yaml}), the Mailpit API and the registration
 * storage contract ({@code registration-storage.sql}). Business values come from the application's
 * configuration (AR-04), never from literals.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"app.test-clock=enabled", "app.rate-limit-per-hour=100000"})
abstract class AcceptanceTestBase {

  static final String POSTGRES_IMAGE = "postgres:16.15-alpine";
  static final String MAILPIT_IMAGE = "axllent/mailpit:v1.31.1";
  static final String ORGANIZER_USERNAME = "organizer-acceptance";
  static final String ORGANIZER_PASSWORD = randomSecret();

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(MAILPIT_IMAGE).withExposedPorts(1025, 8025);

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  static final ObjectMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP = HttpClient.newHttpClient();

  @LocalServerPort int port;

  @Autowired Environment environment;

  @DynamicPropertySource
  static void containers(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("app.organizer.username", () -> ORGANIZER_USERNAME);
    registry.add("app.organizer.password", () -> ORGANIZER_PASSWORD);
  }

  private static String randomSecret() {
    byte[] bytes = new byte[24];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  // ---- configuration (AR-04) ----

  String config(String key) {
    String value = environment.getProperty(key);
    assertThat(value).as("configuration property %s provided by the application", key).isNotBlank();
    return value;
  }

  ZoneId conferenceZone() {
    return ZoneId.of(config("app.conference-tz"));
  }

  LocalDate earlyBirdDeadline() {
    return LocalDate.parse(config("app.early-bird-deadline"));
  }

  BigDecimal feeEarly() {
    return new BigDecimal(config("app.fee-early"));
  }

  BigDecimal feeRegular() {
    return new BigDecimal(config("app.fee-regular"));
  }

  BigDecimal vatRate() {
    return new BigDecimal(config("app.vat-rate"));
  }

  /** Configured workshops in order, id to title ({@code id=title;id=title}). */
  Map<String, String> workshops() {
    Map<String, String> result = new LinkedHashMap<>();
    for (String entry : config("app.workshops").split(";")) {
      String[] parts = entry.split("=", 2);
      result.put(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
    }
    return result;
  }

  int rateLimitPerHour() {
    return Integer.parseInt(config("app.rate-limit-per-hour"));
  }

  // ---- oracle (REQ-REG-01 "Oracle notes") ----

  static BigDecimal vatOf(BigDecimal net, BigDecimal rate) {
    return net.multiply(rate).setScale(2, RoundingMode.HALF_UP);
  }

  /** Noon local time on the given date in the conference time zone. */
  Instant localNoon(LocalDate date) {
    return date.atTime(12, 0).atZone(conferenceZone()).toInstant();
  }

  /** First instant after the early-bird deadline (00:00:00 local on the next day). */
  Instant firstRegularInstant() {
    return earlyBirdDeadline().plusDays(1).atStartOfDay(conferenceZone()).toInstant();
  }

  Instant earlyInstant() {
    return localNoon(earlyBirdDeadline().minusDays(11));
  }

  Instant regularInstant() {
    return localNoon(earlyBirdDeadline().plusDays(5));
  }

  // ---- fixtures (REQ-REG-01 "Data and fixtures"), unique e-mail per test ----

  static String uniqueEmail() {
    return "ana.novak+" + UUID.randomUUID().toString().substring(0, 12) + "@example.org";
  }

  static Map<String, Object> privatePayer(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", email);
    body.put("payerType", "private");
    body.put("workshops", List.of());
    return body;
  }

  static Map<String, Object> sloveneCompany(String email) {
    Map<String, Object> body = privatePayer(email);
    body.put("payerType", "company");
    body.put("companyName", "Primer d.o.o.");
    body.put("companyAddress", "Koroška cesta 1, 2000 Maribor");
    body.put("companyVatId", "SI00000001");
    return body;
  }

  static Map<String, Object> austrianCompany(String email) {
    Map<String, Object> body = privatePayer(email);
    body.put("payerType", "company");
    body.put("companyName", "Beispiel GmbH");
    body.put("companyAddress", "Ringstraße 1, 1010 Wien");
    body.put("companyVatId", "ATU00000001");
    return body;
  }

  // ---- registration API ----

  record ApiResponse(int status, String body, HttpResponse<String> raw) {
    JsonNode json() {
      return JSON.readTree(body);
    }
  }

  URI api(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  ApiResponse postRegistration(Object body, Instant now) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(api("/api/registrations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
    if (now != null) {
      request.header("X-Test-Now", now.toString());
    }
    return send(request.build());
  }

  ApiResponse getRegistration(String registrationNumber, String username, String password) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(
                api(
                    "/api/registrations/"
                        + URLEncoder.encode(registrationNumber, StandardCharsets.UTF_8)))
            .GET();
    if (username != null) {
      String token =
          Base64.getEncoder()
              .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
      request.header("Authorization", "Basic " + token);
    }
    return send(request.build());
  }

  ApiResponse getAsOrganizer(String registrationNumber) {
    return getRegistration(registrationNumber, ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  static ApiResponse send(HttpRequest request) {
    try {
      HttpResponse<String> response =
          HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new ApiResponse(response.statusCode(), response.body(), response);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("HTTP request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request interrupted", e);
    }
  }

  /** Posts a valid registration and returns the stored registration (asserts 201). */
  JsonNode register(Map<String, Object> body, Instant now) {
    ApiResponse response = postRegistration(body, now);
    assertThat(response.status()).as("status of %s", response.body()).isEqualTo(201);
    return response.json();
  }

  static BigDecimal amount(JsonNode registration, String field) {
    JsonNode node = registration.get(field);
    assertThat(node).as("field %s", field).isNotNull();
    assertThat(node.isNull()).as("field %s is not null", field).isFalse();
    BigDecimal value = new BigDecimal(node.asString());
    assertThat(value.stripTrailingZeros().scale())
        .as("%s has at most two decimals", field)
        .isLessThanOrEqualTo(2);
    return value;
  }

  static void assertAmounts(
      JsonNode registration, BigDecimal net, BigDecimal vat, BigDecimal gross) {
    assertThat(amount(registration, "netFee")).isEqualByComparingTo(net);
    assertThat(amount(registration, "vat")).isEqualByComparingTo(vat);
    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(gross);
  }

  // ---- Mailpit API ----

  static String mailpit(String path) {
    return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path;
  }

  static List<JsonNode> mailsTo(String email) {
    String query = URLEncoder.encode("to:\"" + email + "\"", StandardCharsets.UTF_8);
    ApiResponse response =
        send(HttpRequest.newBuilder(URI.create(mailpit("/api/v1/search?query=" + query))).build());
    assertThat(response.status()).as("Mailpit search").isEqualTo(200);
    List<JsonNode> messages = new ArrayList<>();
    response.json().get("messages").forEach(messages::add);
    return messages;
  }

  /** Waits until at least one mail arrived for the address, then a grace period for extras. */
  static List<JsonNode> awaitMailsTo(String email) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(250))
        .until(() -> !mailsTo(email).isEmpty());
    sleep(Duration.ofSeconds(2));
    return mailsTo(email);
  }

  /** Asserts that no mail arrives for the address within a grace period. */
  static void assertNoMailTo(String email) {
    sleep(Duration.ofSeconds(3));
    assertThat(mailsTo(email)).as("mails to %s", email).isEmpty();
  }

  static String mailText(JsonNode message) {
    String id = message.get("ID").asString();
    ApiResponse response =
        send(HttpRequest.newBuilder(URI.create(mailpit("/api/v1/message/" + id))).build());
    assertThat(response.status()).as("Mailpit message").isEqualTo(200);
    return response.json().get("Text").asString();
  }

  static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  // ---- registration storage contract ----

  static int storedRegistrationsFor(String email) {
    return queryInt("SELECT count(*) FROM registration WHERE email = ?", email);
  }

  static List<String> tablesInPublicSchema() {
    List<String> tables = new ArrayList<>();
    try (Connection connection = storage();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
        ResultSet rows = statement.executeQuery()) {
      while (rows.next()) {
        tables.add(rows.getString(1));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("storage query failed", e);
    }
    return tables;
  }

  static int queryInt(String sql, String parameter) {
    try (Connection connection = storage();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, parameter);
      try (ResultSet rows = statement.executeQuery()) {
        rows.next();
        return rows.getInt(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("storage query failed", e);
    }
  }

  static Connection storage() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }
}
