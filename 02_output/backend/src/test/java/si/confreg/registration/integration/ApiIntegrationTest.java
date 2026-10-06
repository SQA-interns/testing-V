package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import si.confreg.registration.application.DuplicateRegistrationNumberException;
import si.confreg.registration.application.RegistrationStore;
import si.confreg.registration.domain.Fee;
import si.confreg.registration.domain.Payer;
import si.confreg.registration.domain.Registration;

/**
 * Integration tests of the HTTP and storage behaviour that the acceptance criteria do not cover:
 * security headers (SB-10), body limit (SR-02), error mapping (SB-07), no personal data in logs
 * (SR-01), unknown paths, and the storage adapter's number uniqueness.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ApiIntegrationTest {

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.15-alpine");

  @SuppressWarnings("resource")
  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1").withExposedPorts(1025, 8025);

  static final String USER = "integration-organizer";
  static final String PASSWORD = "integration-password-" + System.nanoTime();

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("app.test-clock", () -> "enabled");
    registry.add("app.organizer.username", () -> USER);
    registry.add("app.organizer.password", () -> PASSWORD);
  }

  @Autowired Environment environment;
  @Autowired RegistrationStore store;

  private final HttpClient http = HttpClient.newHttpClient();

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + environment.getProperty("local.server.port") + path);
  }

  private HttpResponse<String> post(String body, String contentType, String testNow)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri("/api/registrations"))
            .header("Content-Type", contentType)
            .header("X-Forwarded-For", "10.200." + (System.nanoTime() % 250) + ".9")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
    if (testNow != null) {
      request.header("X-Test-Now", testNow);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path, boolean authenticated) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
    if (authenticated) {
      request.header(
          "Authorization",
          "Basic "
              + Base64.getEncoder()
                  .encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8)));
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static final String VALID =
      "{\"firstName\":\"Zofija\",\"lastName\":\"Žagar\",\"email\":\"zofija.zagar@example.org\","
          + "\"payerType\":\"company\",\"companyName\":\"Integracija d.o.o.\","
          + "\"companyAddress\":\"Testna 9\",\"companyVatId\":\"SI99999999\",\"workshops\":[]}";

  @Test
  void securityHeadersOnApiResponses() throws Exception {
    HttpResponse<String> response = get("/api/workshops", false);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .contains("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Frame-Options")).contains("DENY");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    assertThat(response.headers().firstValue("Referrer-Policy")).contains("no-referrer");
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
  }

  @Test
  void bodyLargerThanTheLimitIs413() throws Exception {
    String padded = VALID.replace("\"workshops\":[]", "\"pad\":\"" + "x".repeat(17_000) + "\"");

    assertThat(post(padded, "application/json", null).statusCode()).isEqualTo(413);
  }

  @Test
  void malformedRequestsGetProblemResponsesWithoutDetails() throws Exception {
    HttpResponse<String> notJson = post("{not json", "application/json", null);
    assertThat(notJson.statusCode()).isEqualTo(400);
    assertThat(notJson.headers().firstValue("Content-Type").orElse(""))
        .startsWith("application/problem+json");
    assertThat(notJson.body()).doesNotContain("Exception").doesNotContain("at ");

    assertThat(post("[1,2]", "application/json", null).statusCode()).isEqualTo(400);
    assertThat(post("\"text\"", "application/json", null).statusCode()).isEqualTo(400);
    assertThat(post(VALID, "text/plain", null).statusCode()).isEqualTo(415);
    assertThat(post(VALID, "application/json", "yesterday").statusCode()).isEqualTo(400);
  }

  @Test
  void unknownApiPathsNeedAuthenticationThenAre404() throws Exception {
    assertThat(get("/api/nothing-here", false).statusCode()).isEqualTo(401);
    assertThat(get("/api/nothing-here", false).headers().firstValue("WWW-Authenticate"))
        .contains("Basic realm=\"confreg\"");
    assertThat(get("/api/nothing-here", true).statusCode()).isEqualTo(404);
    assertThat(get("/api/registrations/not-a-number", true).statusCode()).isEqualTo(404);
  }

  @Test
  void personalDataNeverReachesTheLog(CapturedOutput output) throws Exception {
    HttpResponse<String> created = post(VALID, "application/json", "2031-03-03T10:00:00Z");
    assertThat(created.statusCode()).isEqualTo(201);
    post(VALID.replace("zofija.zagar@example.org", "bad"), "application/json", null);

    assertThat(output.getAll())
        .doesNotContain("Zofija")
        .doesNotContain("Žagar")
        .doesNotContain("zofija.zagar@example.org")
        .doesNotContain("Integracija")
        .doesNotContain("SI99999999")
        .doesNotContain(PASSWORD);
  }

  @Test
  void storeRejectsADuplicateRegistrationNumber() {
    Registration registration =
        new Registration(
            "REG-1NTEGRAT10",
            "A",
            "B",
            "a@b.co",
            Payer.privatePerson(),
            null,
            new Fee(new BigDecimal("1.00"), new BigDecimal("0.00"), new BigDecimal("1.00")),
            Instant.parse("2031-01-01T00:00:00Z"));
    store.add(registration);

    assertThatThrownBy(() -> store.add(registration))
        .isInstanceOf(DuplicateRegistrationNumberException.class);
    assertThat(store.findByNumber(registration.registrationNumber())).contains(registration);
  }
}
