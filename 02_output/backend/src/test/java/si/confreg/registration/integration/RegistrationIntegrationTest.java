package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalManagementPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** NFR-01, SR-01, SR-02, SB-07, SB-10, D-15 against the running backend. */
@ExtendWith(OutputCaptureExtension.class)
class RegistrationIntegrationTest extends IntegrationTestBase {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @LocalManagementPort int managementPort;

  private static String body(String email) {
    return """
        {"firstName":"Žiga","lastName":"Čašič","email":"%s","payerType":"company",
         "companyName":"Šumi d.o.o.","companyAddress":"Koroška cesta 1, 2000 Maribor",
         "companyVatId":"SI00000001","workshops":[]}"""
        .formatted(email);
  }

  @Test
  void slovenianCharactersSurviveStorageAndEmailAndNoPersonalDataIsLogged(CapturedOutput output)
      throws Exception {
    String email = "ziga+" + UUID.randomUUID().toString().substring(0, 8) + "@example.org";

    HttpResponse<String> created = post(body(email));

    assertThat(created.statusCode()).isEqualTo(201);
    String number = JSON.readTree(created.body()).get("registrationNumber").asString();
    JsonNode stored =
        JSON.readTree(get("/api/registrations/" + number, ORGANIZER, PASSWORD).body());
    assertThat(stored.get("firstName").asString()).isEqualTo("Žiga");
    assertThat(stored.get("lastName").asString()).isEqualTo("Čašič");
    assertThat(stored.get("companyName").asString()).isEqualTo("Šumi d.o.o.");
    assertThat(stored.get("companyAddress").asString()).isEqualTo("Koroška cesta 1, 2000 Maribor");

    String query = URLEncoder.encode("to:\"" + email + "\"", StandardCharsets.UTF_8);
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () ->
                JSON.readTree(send(mailpitGet("/api/v1/search?query=" + query)).body())
                        .get("messages")
                        .size()
                    == 1);
    String id =
        JSON.readTree(send(mailpitGet("/api/v1/search?query=" + query)).body())
            .get("messages")
            .get(0)
            .get("ID")
            .asString();
    String text =
        JSON.readTree(send(mailpitGet("/api/v1/message/" + id)).body()).get("Text").asString();
    assertThat(text).contains("Dear Žiga Čašič,").contains("Šumi d.o.o., Koroška cesta 1");

    post("{\"firstName\":\"Žiga\",\"email\":\"" + email + "\"}");
    assertThat(output.getAll())
        .doesNotContain(email)
        .doesNotContain("Čašič")
        .doesNotContain("Koroška")
        .doesNotContain("SI00000001");
  }

  private static HttpRequest mailpitGet(String path) {
    return HttpRequest.newBuilder(URI.create(mailpit(path))).GET().build();
  }

  @Test
  void confirmationIsMarkedSentAfterOneAttemptWithUtcTimestamps() throws Exception {
    String email = "sent+" + UUID.randomUUID().toString().substring(0, 8) + "@example.org";
    HttpResponse<String> created =
        send(
            request("/api/registrations")
                .header("Content-Type", "application/json")
                .header("X-Test-Now", "2026-07-31T21:59:59Z")
                .POST(HttpRequest.BodyPublishers.ofString(body(email), StandardCharsets.UTF_8))
                .build());
    assertThat(created.statusCode()).isEqualTo(201);

    try (java.sql.Connection connection =
            java.sql.DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        java.sql.PreparedStatement statement =
            connection.prepareStatement(
                "SELECT confirmation_status, confirmation_attempts, submitted_at"
                    + " FROM registration WHERE email = ?")) {
      statement.setString(1, email);
      Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .until(
              () -> {
                try (java.sql.ResultSet rows = statement.executeQuery()) {
                  return rows.next() && "sent".equals(rows.getString(1));
                }
              });
      try (java.sql.ResultSet rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getInt(2)).isEqualTo(1);
        assertThat(rows.getObject(3, java.time.OffsetDateTime.class).toInstant())
            .isEqualTo(java.time.Instant.parse("2026-07-31T21:59:59Z"));
      }
    }
  }

  @Test
  void apiResponsesCarrySecurityHeaders() throws Exception {
    HttpResponse<String> response = post("{}");

    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .hasValue("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
    assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
  }

  @Test
  void bodyThatIsNotAJsonObjectAnswers400() throws Exception {
    assertThat(post("[1,2]").statusCode()).isEqualTo(400);
    assertThat(post("not json").statusCode()).isEqualTo(400);
  }

  @Test
  void bodyOver16KibAnswers413() throws Exception {
    String large = "{\"firstName\":\"" + "a".repeat(17 * 1024) + "\"}";
    assertThat(post(large).statusCode()).isEqualTo(413);
  }

  @Test
  void nonJsonContentTypeAnswers415() throws Exception {
    HttpResponse<String> response =
        send(
            request("/api/registrations")
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("x"))
                .build());
    assertThat(response.statusCode()).isEqualTo(415);
  }

  @Test
  void invalidTestClockHeaderAnswers400() throws Exception {
    HttpResponse<String> response =
        send(
            request("/api/registrations")
                .header("Content-Type", "application/json")
                .header("X-Test-Now", "tomorrow")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build());
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void errorsExposeNoInternals() throws Exception {
    HttpResponse<String> response = post("not json");
    assertThat(response.body())
        .doesNotContain("Exception")
        .doesNotContain("at si.")
        .doesNotContain("tools.jackson");
  }

  @Test
  void otherPathsNeedTheOrganizer() throws Exception {
    assertThat(get("/api/anything", null, null).statusCode()).isEqualTo(401);
    assertThat(get("/actuator/env", null, null).statusCode()).isEqualTo(401);
  }

  @Test
  void healthAndReadinessAreOnTheManagementPortWithoutDetails() throws Exception {
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + managementPort + "/actuator/health/readiness"))
                .GET()
                .build());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"").doesNotContain("components");
    assertThat(get("/actuator/health", null, null).statusCode()).isEqualTo(404);
  }
}
