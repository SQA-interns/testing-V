package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box harness for the acceptance tests (docs/01_acceptance-criteria.md).
 *
 * <p>Talks to the backend only through its public HTTP API (registration-api.openapi.yaml), to the
 * mail catcher through the Mailpit API, and reads the registration table defined by the storage
 * contract (registration-storage.sql) only to prove that nothing was stored. Business values are
 * read from the configuration the backend runs with, never written as literals.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AcceptanceTestBase {

  private static final SecureRandom RANDOM = new SecureRandom();

  static final String POSTGRES_IMAGE = "postgres:16.15-alpine";
  static final String MAILPIT_IMAGE = "axllent/mailpit:v1.31.1";
  static final int MAILPIT_SMTP_PORT = 1025;
  static final int MAILPIT_HTTP_PORT = 8025;

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(MAILPIT_IMAGE).withExposedPorts(MAILPIT_SMTP_PORT, MAILPIT_HTTP_PORT);

  static final String ORGANIZER_USERNAME = "organizer-" + randomToken(6);
  static final String ORGANIZER_PASSWORD = randomToken(24);

  /** Business settings the backend is started with (from environments.md). */
  static final ConferenceSettings SETTINGS = ConferenceSettings.load();

  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(MAILPIT_SMTP_PORT));
    registry.add("app.test-clock", () -> "enabled");
    registry.add("app.organizer.username", () -> ORGANIZER_USERNAME);
    registry.add("app.organizer.password", () -> ORGANIZER_PASSWORD);
    registry.add("app.conference-tz", () -> SETTINGS.get("APP_CONFERENCE_TZ"));
    registry.add("app.early-bird-deadline", () -> SETTINGS.get("APP_EARLY_BIRD_DEADLINE"));
    registry.add("app.fee-early", () -> SETTINGS.get("APP_FEE_EARLY"));
    registry.add("app.fee-regular", () -> SETTINGS.get("APP_FEE_REGULAR"));
    registry.add("app.vat-rate", () -> SETTINGS.get("APP_VAT_RATE"));
    registry.add("app.workshops", SETTINGS::workshopsProperty);
    registry.add("app.rate-limit-per-hour", () -> SETTINGS.get("APP_RATE_LIMIT_PER_HOUR"));
  }

  @Autowired Environment environment;

  /** Unique client address per test, so the per-client rate limit never couples tests. */
  String clientAddress;

  @BeforeEach
  void resetMailboxAndClient() throws Exception {
    HttpResponse<String> deleted =
        HTTP.send(
            HttpRequest.newBuilder(mailpitUri("/api/v1/messages")).DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(deleted.statusCode()).as("Mailpit reset").isEqualTo(200);
    clientAddress = randomClientAddress();
  }

  // ---------------------------------------------------------------- configuration

  String config(String setting) {
    return SETTINGS.get(setting);
  }

  int serverPort() {
    String port = environment.getProperty("local.server.port");
    assertThat(port).as("backend server port").isNotBlank();
    return Integer.parseInt(port);
  }

  BigDecimal earlyFee() {
    return new BigDecimal(config("APP_FEE_EARLY")).setScale(2, RoundingMode.HALF_UP);
  }

  BigDecimal regularFee() {
    return new BigDecimal(config("APP_FEE_REGULAR")).setScale(2, RoundingMode.HALF_UP);
  }

  BigDecimal vatOf(BigDecimal net) {
    return net.multiply(new BigDecimal(config("APP_VAT_RATE"))).setScale(2, RoundingMode.HALF_UP);
  }

  ZoneId conferenceZone() {
    return ZoneId.of(config("APP_CONFERENCE_TZ"));
  }

  LocalDate earlyBirdDeadline() {
    return LocalDate.parse(config("APP_EARLY_BIRD_DEADLINE"));
  }

  /** Last second of the early-bird deadline day in the conference time zone. */
  Instant lastEarlyBirdSecond() {
    return earlyBirdDeadline()
        .plusDays(1)
        .atStartOfDay(conferenceZone())
        .minusSeconds(1)
        .toInstant();
  }

  /** First instant after the early-bird deadline day in the conference time zone. */
  Instant firstRegularInstant() {
    return earlyBirdDeadline().plusDays(1).atStartOfDay(conferenceZone()).toInstant();
  }

  /** Noon in the conference zone, the given number of days before or after the deadline. */
  Instant daysFromDeadline(int days) {
    return earlyBirdDeadline().plusDays(days).atTime(12, 0).atZone(conferenceZone()).toInstant();
  }

  /** Configured workshop ids in configured order ({@code id=title;id=title}). */
  List<String> workshopIds() {
    return new ArrayList<>(workshops().keySet());
  }

  Map<String, String> workshops() {
    return SETTINGS.workshops();
  }

  int rateLimitPerHour() {
    return Integer.parseInt(config("APP_RATE_LIMIT_PER_HOUR"));
  }

  // ---------------------------------------------------------------- request bodies

  static Map<String, Object> privatePayer(String firstName, String lastName, String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("firstName", firstName);
    body.put("lastName", lastName);
    body.put("email", email);
    body.put("payerType", "private");
    body.put("workshops", List.of());
    return body;
  }

  /** Fixture participant Ana Novak, private payer (REQ-REG-01 "Data and fixtures"). */
  static Map<String, Object> anaPrivate() {
    return privatePayer("Ana", "Novak", "ana.novak@example.org");
  }

  /** Fixture participant with the Slovenian company payer Primer d.o.o. */
  static Map<String, Object> anaForSlovenianCompany() {
    Map<String, Object> body = anaPrivate();
    body.put("payerType", "company");
    body.put("companyName", "Primer d.o.o.");
    body.put("companyAddress", "Koroška cesta 1, 2000 Maribor");
    body.put("companyVatId", "SI00000001");
    return body;
  }

  /** Fixture participant with the Austrian company payer Beispiel GmbH. */
  static Map<String, Object> anaForAustrianCompany() {
    Map<String, Object> body = anaPrivate();
    body.put("payerType", "company");
    body.put("companyName", "Beispiel GmbH");
    body.put("companyAddress", "Ringstraße 1, 1010 Wien");
    body.put("companyVatId", "ATU00000001");
    return body;
  }

  // ---------------------------------------------------------------- HTTP

  record ApiResponse(int status, Map<String, List<String>> headers, String body) {

    JsonNode json() {
      return JSON.readTree(body);
    }

    String header(String name) {
      return headers.entrySet().stream()
          .filter(e -> e.getKey().equalsIgnoreCase(name))
          .flatMap(e -> e.getValue().stream())
          .findFirst()
          .orElse(null);
    }
  }

  URI apiUri(String path) {
    return URI.create("http://127.0.0.1:" + serverPort() + path);
  }

  ApiResponse register(Map<String, Object> body, Instant now) {
    return postRegistration(JSON.writeValueAsString(body), now, clientAddress);
  }

  ApiResponse postRegistration(String rawJson, Instant now, String client) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(apiUri("/api/registrations"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("X-Forwarded-For", client)
            .POST(HttpRequest.BodyPublishers.ofString(rawJson, StandardCharsets.UTF_8));
    if (now != null) {
      request.header("X-Test-Now", now.toString());
    }
    return send(request.build());
  }

  ApiResponse getRegistrationAsOrganizer(String registrationNumber) {
    return getRegistration(registrationNumber, ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  ApiResponse getRegistration(String registrationNumber, String username, String password) {
    return get("/api/registrations/" + registrationNumber, username, password);
  }

  ApiResponse getAsOrganizer(String path) {
    return get(path, ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  ApiResponse get(String path, String username, String password) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(apiUri(path))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("X-Forwarded-For", clientAddress)
            .GET();
    if (username != null) {
      String token =
          Base64.getEncoder()
              .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
      request.header("Authorization", "Basic " + token);
    }
    return send(request.build());
  }

  ApiResponse getWorkshops() {
    return send(
        HttpRequest.newBuilder(apiUri("/api/workshops"))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("X-Forwarded-For", clientAddress)
            .GET()
            .build());
  }

  static ApiResponse send(HttpRequest request) {
    try {
      HttpResponse<String> response =
          HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new ApiResponse(response.statusCode(), response.headers().map(), response.body());
    } catch (java.io.IOException e) {
      throw new IllegalStateException("HTTP request failed: " + request.uri(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request interrupted", e);
    }
  }

  /** Registers and asserts 201 with a registration number; returns the response. */
  ApiResponse registerSuccessfully(Map<String, Object> body, Instant now) {
    ApiResponse response = register(body, now);
    assertThat(response.status())
        .as("POST /api/registrations status, body: %s", response.body())
        .isEqualTo(201);
    assertThat(registrationNumberOf(response)).isNotBlank();
    return response;
  }

  static String registrationNumberOf(ApiResponse response) {
    return response.json().path("registrationNumber").asString("");
  }

  /** Reads the stored registration through the organizer endpoint and asserts 200. */
  ApiResponse storedRegistration(String registrationNumber) {
    ApiResponse response = getRegistrationAsOrganizer(registrationNumber);
    assertThat(response.status())
        .as("GET /api/registrations/%s status, body: %s", registrationNumber, response.body())
        .isEqualTo(200);
    return response;
  }

  static BigDecimal amount(JsonNode node, String field) {
    JsonNode value = node.path(field);
    assertThat(value.isMissingNode() || value.isNull())
        .as("amount field %s present", field)
        .isFalse();
    return new BigDecimal(value.asString());
  }

  /** Amount as written in the raw JSON: a number or a string with exactly two decimals. */
  static void assertTwoDecimals(String rawJson, String field) {
    assertThat(rawJson)
        .as("%s is written with two decimals", field)
        .containsPattern("\"" + field + "\"\\s*:\\s*\"?-?\\d+\\.\\d{2}\"?\\s*[,}]");
  }

  void assertFee(ApiResponse response, BigDecimal net) {
    BigDecimal vat = vatOf(net);
    JsonNode registration = response.json();
    assertThat(amount(registration, "netFee")).as("netFee").isEqualByComparingTo(net);
    assertThat(amount(registration, "vat")).as("vat").isEqualByComparingTo(vat);
    assertThat(amount(registration, "grossFee")).as("grossFee").isEqualByComparingTo(net.add(vat));
    for (String field : List.of("netFee", "vat", "grossFee")) {
      assertTwoDecimals(response.body(), field);
    }
  }

  // ---------------------------------------------------------------- storage contract

  /** Number of rows in the contract table {@code registration}; 0 if it does not exist yet. */
  static long storedRegistrationCount() {
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement statement = connection.createStatement()) {
      try (ResultSet exists =
          statement.executeQuery("SELECT to_regclass('public.registration') IS NOT NULL")) {
        exists.next();
        if (!exists.getBoolean(1)) {
          return 0;
        }
      }
      try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM registration")) {
        rows.next();
        return rows.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("storage contract query failed", e);
    }
  }

  // ---------------------------------------------------------------- mail

  URI mailpitUri(String path) {
    return URI.create(
        "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(MAILPIT_HTTP_PORT) + path);
  }

  /** Messages in Mailpit addressed to {@code recipient}, newest first. */
  List<JsonNode> messagesTo(String recipient) {
    ApiResponse list =
        send(HttpRequest.newBuilder(mailpitUri("/api/v1/messages?limit=500")).GET().build());
    assertThat(list.status()).as("Mailpit list").isEqualTo(200);
    List<JsonNode> result = new ArrayList<>();
    for (JsonNode message : list.json().path("messages")) {
      for (JsonNode to : message.path("To")) {
        if (recipient.equalsIgnoreCase(to.path("Address").asString(""))) {
          result.add(message);
        }
      }
    }
    return result;
  }

  /** Waits until at least {@code expected} messages arrived (max 10 s), then settles briefly. */
  List<JsonNode> awaitMessagesTo(String recipient, int expected) {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    List<JsonNode> messages = messagesTo(recipient);
    while (messages.size() < expected && System.nanoTime() < deadline) {
      pause(Duration.ofMillis(200));
      messages = messagesTo(recipient);
    }
    pause(Duration.ofMillis(750));
    return messagesTo(recipient);
  }

  /** Total messages in Mailpit after a settle period, to prove that no e-mail was sent. */
  int totalMessagesAfterSettle() {
    pause(Duration.ofSeconds(1));
    ApiResponse list =
        send(HttpRequest.newBuilder(mailpitUri("/api/v1/messages?limit=1")).GET().build());
    assertThat(list.status()).as("Mailpit list").isEqualTo(200);
    return list.json().path("messages_count").asInt(list.json().path("total").asInt());
  }

  /** Full message (subject, text body, headers) by Mailpit id. */
  JsonNode message(JsonNode summary) {
    ApiResponse full =
        send(
            HttpRequest.newBuilder(mailpitUri("/api/v1/message/" + summary.path("ID").asString()))
                .GET()
                .build());
    assertThat(full.status()).as("Mailpit message").isEqualTo(200);
    return full.json();
  }

  // ---------------------------------------------------------------- helpers

  static void pause(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  static String randomToken(int length) {
    String alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder token = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      token.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
    }
    return token.toString();
  }

  static String randomClientAddress() {
    return "10."
        + (1 + RANDOM.nextInt(254))
        + "."
        + RANDOM.nextInt(256)
        + "."
        + (1 + RANDOM.nextInt(254));
  }
}
