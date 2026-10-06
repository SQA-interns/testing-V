package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * HTTP behaviour of the security and error rules (SB-02, SB-07, SB-10, SR-01, SR-02, SR-04 at
 * runtime, D-26), with the real application, PostgreSQL and Mailpit.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0")
@ExtendWith(OutputCaptureExtension.class)
class HttpIntegrationTest {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8");
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/livez").forPort(8025));
  static final String USER = "organizer-" + UUID.randomUUID();
  static final String PASSWORD = UUID.randomUUID().toString();

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  @LocalServerPort int port;
  @LocalManagementPort int managementPort;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("SPRING_DATASOURCE_URL", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("POSTGRES_PASSWORD", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("ORGANIZER_USERNAME", () -> USER);
    registry.add("ORGANIZER_PASSWORD", () -> PASSWORD);
    registry.add("APP_TEST_CLOCK", () -> "enabled");
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return HTTP.send(
        request.timeout(Duration.ofSeconds(30)).build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private HttpRequest.Builder at(String path) {
    return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
  }

  private HttpResponse<String> post(String body, String contentType) throws Exception {
    return send(
        at("/api/registrations")
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)));
  }

  private static String basic(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void securityHeadersOnApiResponses() throws Exception {
    HttpResponse<String> response = send(at("/api/workshops").GET());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .hasValue("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
    assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
  }

  @Test
  void organizerEndpointChallengesWithoutAndWithWrongCredentials() throws Exception {
    HttpResponse<String> none = send(at("/api/registrations/REG-999999999").GET());
    HttpResponse<String> wrong =
        send(
            at("/api/registrations/REG-999999999")
                .header("Authorization", basic(USER, "wrong-password-123"))
                .GET());
    HttpResponse<String> right =
        send(
            at("/api/registrations/REG-999999999")
                .header("Authorization", basic(USER, PASSWORD))
                .GET());

    assertThat(none.statusCode()).isEqualTo(401);
    assertThat(none.headers().firstValue("WWW-Authenticate")).hasValue("Basic realm=\"confreg\"");
    assertThat(none.body()).isEqualTo("{\"error\":\"unauthorized\"}");
    assertThat(wrong.statusCode()).isEqualTo(401);
    assertThat(right.statusCode()).isEqualTo(404);
    assertThat(right.body()).isEqualTo("{\"error\":\"not_found\"}");
  }

  @Test
  void otherApiPathsNeedTheOrganizer() throws Exception {
    assertThat(send(at("/api/registrations").GET()).statusCode()).isEqualTo(401);
    assertThat(send(at("/api/anything").GET()).statusCode()).isEqualTo(401);
    assertThat(
            send(at("/api/anything").header("Authorization", basic(USER, PASSWORD)).GET())
                .statusCode())
        .isEqualTo(404);
  }

  @Test
  void malformedNotJsonAndOversizedBodies() throws Exception {
    HttpResponse<String> malformed = post("{", "application/json");
    HttpResponse<String> notJson = post("firstName=Ana", "application/x-www-form-urlencoded");
    HttpResponse<String> large =
        post("{\"firstName\":\"" + "a".repeat(17_000) + "\"}", "application/json");

    assertThat(malformed.statusCode()).isEqualTo(400);
    assertThat(malformed.body()).isEqualTo("{\"error\":\"malformed_request\"}");
    assertThat(notJson.statusCode()).isEqualTo(415);
    assertThat(notJson.body()).isEqualTo("{\"error\":\"unsupported_media_type\"}");
    assertThat(large.statusCode()).isEqualTo(413);
    assertThat(large.body()).isEqualTo("{\"error\":\"payload_too_large\"}");
  }

  @Test
  void malformedTestClockHeaderIsRejected() throws Exception {
    HttpResponse<String> response =
        send(
            at("/api/registrations")
                .header("Content-Type", "application/json")
                .header("X-Test-Now", "tomorrow")
                .POST(HttpRequest.BodyPublishers.ofString("{}")));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).isEqualTo("{\"error\":\"invalid_test_clock\"}");
  }

  @Test
  void nonApiPathsAreNotFoundWithoutDetails() throws Exception {
    HttpResponse<String> response = send(at("/actuator/health").GET());

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body()).doesNotContainIgnoringCase("exception");
  }

  @Test
  void healthAndReadinessOnManagementPortOnly() throws Exception {
    HttpRequest.Builder readiness =
        HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:" + managementPort + "/actuator/health/readiness"));
    HttpRequest.Builder liveness =
        HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:" + managementPort + "/actuator/health/liveness"));
    HttpRequest.Builder env =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + "/actuator/env"));

    HttpResponse<String> ready = send(readiness.GET());
    assertThat(ready.statusCode()).isEqualTo(200);
    assertThat(ready.body()).contains("UP").doesNotContain("postgres");
    assertThat(send(liveness.GET()).statusCode()).isEqualTo(200);
    assertThat(send(env.GET()).statusCode()).isEqualTo(404);
  }

  @Test
  void personalDataIsNotLogged(CapturedOutput output) throws Exception {
    String email = "pd-" + UUID.randomUUID() + "@example.com";
    String body =
        "{\"firstName\":\"Logdetektor\",\"lastName\":\"Žagarjeva\",\"email\":\""
            + email
            + "\",\"payerType\":\"company\",\"companyName\":\"Skrivnost d.o.o.\","
            + "\"companyAddress\":\"Tajna ulica 7\",\"companyVatId\":\"SI99887766\"}";

    assertThat(post(body, "application/json").statusCode()).isEqualTo(201);
    assertThat(post(body.replace("company\"", "private\""), "application/json").statusCode())
        .isEqualTo(400);

    assertThat(output.getAll())
        .doesNotContain("Logdetektor")
        .doesNotContain("Žagarjeva")
        .doesNotContain(email)
        .doesNotContain("Skrivnost")
        .doesNotContain("Tajna ulica")
        .doesNotContain("SI99887766")
        .doesNotContain(PASSWORD);
  }
}
