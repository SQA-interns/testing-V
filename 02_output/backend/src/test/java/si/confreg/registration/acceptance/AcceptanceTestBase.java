package si.confreg.registration.acceptance;

import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Black-box harness for the acceptance tests: the backend runs on a random port with PostgreSQL and
 * Mailpit in containers (environments.md, "test"); tests talk to it only over HTTP, read e-mail
 * through the Mailpit API and read storage through the storage contract (docs/02_contracts).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AcceptanceTestBase {

  static final Properties CONFIG = loadConfig();
  static final String ORGANIZER_USERNAME = "organizer-" + UUID.randomUUID();
  static final String ORGANIZER_PASSWORD = UUID.randomUUID().toString();
  static final String NUMBER_PATTERN = "^REG-[0-9]{6,}$";

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8");
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/livez").forPort(8025));

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;

  @DynamicPropertySource
  static void backendProperties(DynamicPropertyRegistry registry) {
    CONFIG
        .stringPropertyNames()
        .forEach(name -> registry.add(name, () -> CONFIG.getProperty(name)));
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("SPRING_DATASOURCE_URL", POSTGRES::getJdbcUrl);
    registry.add("SPRING_DATASOURCE_USERNAME", POSTGRES::getUsername);
    registry.add("POSTGRES_PASSWORD", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("SPRING_MAIL_HOST", MAILPIT::getHost);
    registry.add("SPRING_MAIL_PORT", () -> MAILPIT.getMappedPort(1025));
    registry.add("APP_MAIL_STARTTLS", () -> "false");
    registry.add("ORGANIZER_USERNAME", () -> ORGANIZER_USERNAME);
    registry.add("ORGANIZER_PASSWORD", () -> ORGANIZER_PASSWORD);
  }

  // ---- configuration (AR-04): expectations derive from the injected values ----

  static Properties loadConfig() {
    Properties p = new Properties();
    try (InputStream in =
        AcceptanceTestBase.class.getResourceAsStream("/acceptance/acceptance-test.properties")) {
      p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return p;
  }

  static BigDecimal config(String name) {
    return new BigDecimal(CONFIG.getProperty(name));
  }

  static ZoneId conferenceZone() {
    return ZoneId.of(CONFIG.getProperty("APP_CONFERENCE_TZ"));
  }

  static LocalDate deadline() {
    return LocalDate.parse(CONFIG.getProperty("APP_EARLY_BIRD_DEADLINE"));
  }

  /** First instant of the deadline day in the conference time zone. */
  static Instant deadlineDayStart() {
    return deadline().atStartOfDay(conferenceZone()).toInstant();
  }

  /** Last full second of the deadline day in the conference time zone. */
  static Instant deadlineDayLastSecond() {
    return deadline().plusDays(1).atStartOfDay(conferenceZone()).toInstant().minusSeconds(1);
  }

  /** First instant after the deadline day in the conference time zone. */
  static Instant afterDeadline() {
    return deadline().plusDays(1).atStartOfDay(conferenceZone()).toInstant();
  }

  static BigDecimal expectedVat(BigDecimal net) {
    return net.multiply(config("APP_VAT_RATE")).setScale(2, RoundingMode.HALF_UP);
  }

  static List<String> workshopIds() {
    return java.util.Arrays.stream(CONFIG.getProperty("APP_WORKSHOPS").split(";"))
        .map(pair -> pair.split("=", 2)[0].trim())
        .toList();
  }

  static String workshopTitle(String id) {
    return java.util.Arrays.stream(CONFIG.getProperty("APP_WORKSHOPS").split(";"))
        .map(pair -> pair.split("=", 2))
        .filter(pair -> pair[0].trim().equals(id))
        .map(pair -> pair[1].trim())
        .findFirst()
        .orElseThrow();
  }

  // ---- request bodies (NFR-01: Slovenian characters) ----

  static String uniqueEmail(String tag) {
    return tag + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
  }

  static Map<String, Object> privateRegistration(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("firstName", "Špela");
    body.put("lastName", "Žagar Čeč");
    body.put("email", email);
    body.put("payerType", "private");
    body.put("companyName", null);
    body.put("companyAddress", null);
    body.put("companyVatId", null);
    body.put("workshops", List.of());
    return body;
  }

  static Map<String, Object> companyRegistration(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("firstName", "Matej");
    body.put("lastName", "Šuštar");
    body.put("email", email);
    body.put("payerType", "company");
    body.put("companyName", "Žičnice Čatež d.o.o.");
    body.put("companyAddress", "Šmartinska cesta 152, 1000 Ljubljana");
    body.put("companyVatId", "SI12345678");
    body.put("workshops", List.of());
    return body;
  }

  // ---- HTTP ----

  String baseUrl() {
    return "http://127.0.0.1:" + port;
  }

  HttpResponse<String> postRegistration(Map<String, Object> body, Instant now) {
    return postRaw(JSON.writeValueAsString(body), now);
  }

  HttpResponse<String> postRaw(String json, Instant now) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/registrations"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
    if (now != null) {
      request.header("X-Test-Now", now.toString());
    }
    return send(request.build());
  }

  HttpResponse<String> getRegistration(String registrationNumber, boolean asOrganizer) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/registrations/" + registrationNumber))
            .timeout(Duration.ofSeconds(30))
            .GET();
    if (asOrganizer) {
      String token = ORGANIZER_USERNAME + ":" + ORGANIZER_PASSWORD;
      request.header(
          "Authorization",
          "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
    }
    return send(request.build());
  }

  static HttpResponse<String> send(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  static Object json(HttpResponse<String> response, String path) {
    return JsonPath.read(response.body(), path);
  }

  static BigDecimal amount(HttpResponse<String> response, String field) {
    Object value = json(response, "$." + field);
    return new BigDecimal(String.valueOf(value));
  }

  // ---- storage contract (docs/02_contracts/registration-storage.sql) ----

  static long storedRegistrations() {
    return queryLong("SELECT count(*) FROM registration", null);
  }

  static long storedRegistrationsFor(String email) {
    return queryLong("SELECT count(*) FROM registration WHERE email = ?", email);
  }

  private static long queryLong(String sql, String parameter) {
    try (Connection c =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        PreparedStatement statement = c.prepareStatement(sql)) {
      if (parameter != null) {
        statement.setString(1, parameter);
      }
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      // a missing table means nothing has been stored
      if ("42P01".equals(e.getSQLState())) {
        return 0;
      }
      throw new IllegalStateException(e);
    }
  }

  // ---- e-mail through the Mailpit API ----

  record Mail(String to, String subject, String text) {}

  static String mailpitUrl() {
    return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
  }

  static List<String> mailIdsTo(String address) {
    String query = URLEncoder.encode("to:\"" + address + "\"", StandardCharsets.UTF_8);
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(URI.create(mailpitUrl() + "/api/v1/search?query=" + query))
                .GET()
                .build());
    return JsonPath.read(response.body(), "$.messages[*].ID");
  }

  static Mail awaitSingleMailTo(String address) {
    AtomicReference<List<String>> ids = new AtomicReference<>();
    await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () -> {
              ids.set(mailIdsTo(address));
              return !ids.get().isEmpty();
            });
    HttpResponse<String> message =
        send(
            HttpRequest.newBuilder(URI.create(mailpitUrl() + "/api/v1/message/" + ids.get().get(0)))
                .GET()
                .build());
    String to = JsonPath.read(message.body(), "$.To[0].Address");
    String subject = JsonPath.read(message.body(), "$.Subject");
    String text = JsonPath.read(message.body(), "$.Text");
    return new Mail(to, subject, text);
  }

  static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
