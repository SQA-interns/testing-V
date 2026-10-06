package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;

/**
 * HTTP-level checks of controls without an acceptance criterion: error mapping (ES-07), body size
 * (SR-02), test clock input, security headers (SB-10), health (ES-09). Reuses the acceptance
 * harness's running backend.
 */
class ApiProtectionIntegrationTest extends AcceptanceTestBase {

  private final HttpClient http = HttpClient.newHttpClient();

  @Autowired private Environment environment;

  private HttpResponse<String> send(HttpRequest.Builder builder)
      throws IOException, InterruptedException {
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + environment.getProperty("local.server.port") + path);
  }

  private HttpRequest.Builder post(String body, String contentType) {
    return HttpRequest.newBuilder(uri("/api/registrations"))
        .header("Content-Type", contentType)
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
  }

  @Test
  void malformedJsonIs400ProblemWithoutInternals() throws Exception {
    HttpResponse<String> response = send(post("{\"firstName\": ", "application/json"));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(v -> assertThat(v).startsWith("application/problem+json"));
    assertThat(response.body()).doesNotContain("Exception").doesNotContain("jackson");
  }

  @Test
  void nonJsonBodyIs415() throws Exception {
    assertThat(send(post("firstName=Ana", "application/x-www-form-urlencoded")).statusCode())
        .isEqualTo(415);
  }

  @Test
  void wrongJsonTypesAreFieldErrors() throws Exception {
    HttpResponse<String> response =
        send(
            post(
                "{\"firstName\":1,\"lastName\":\"N\",\"email\":\"a@b.org\",\"payerType\":\"private\",\"student\":\"yes\"}",
                "application/json"));

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body())
        .contains("\"field\":\"firstName\"")
        .contains("\"field\":\"student\"");
  }

  @Test
  void sr02_bodyOver16KibIs413() throws Exception {
    String big = "{\"firstName\":\"" + "a".repeat(17 * 1024) + "\"}";

    assertThat(send(post(big, "application/json")).statusCode()).isEqualTo(413);
  }

  @Test
  void invalidTestTimeIs400() throws Exception {
    HttpResponse<String> response =
        send(
            HttpRequest.newBuilder(uri("/api/registration-options"))
                .header("X-Test-Now", "soon")
                .GET());

    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void optionsListWorkshopsAndCurrentPrice() throws Exception {
    HttpResponse<String> response =
        send(HttpRequest.newBuilder(uri("/api/registration-options?student=true")).GET());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"tier\":\"student\"").contains("\"grossFee\":0.00");
  }

  @Test
  void invalidStudentParameterIs400() throws Exception {
    assertThat(
            send(HttpRequest.newBuilder(uri("/api/registration-options?student=maybe")).GET())
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void sb10_apiResponsesCarrySecurityHeaders() throws Exception {
    HttpResponse<String> response =
        send(HttpRequest.newBuilder(uri("/api/registration-options")).GET());

    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .hasValue("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
    assertThat(response.headers().firstValue("X-Frame-Options")).hasValue("DENY");
    assertThat(response.headers().firstValue("Referrer-Policy")).hasValue("no-referrer");
  }

  @Test
  void es09_healthIsPublicWithoutDetails() throws Exception {
    HttpResponse<String> health = send(HttpRequest.newBuilder(uri("/actuator/health")).GET());
    HttpResponse<String> readiness =
        send(HttpRequest.newBuilder(uri("/actuator/health/readiness")).GET());

    assertThat(health.statusCode()).isEqualTo(200);
    assertThat(health.body())
        .contains("\"status\":\"UP\"")
        .doesNotContain("components")
        .doesNotContain("details");
    assertThat(readiness.statusCode()).isEqualTo(200);
  }

  @Test
  void otherPathsNeedTheOrganizer() throws Exception {
    assertThat(send(HttpRequest.newBuilder(uri("/actuator/env")).GET()).statusCode())
        .isEqualTo(401);
    assertThat(send(HttpRequest.newBuilder(uri("/api/unknown")).GET()).statusCode()).isEqualTo(401);
  }
}
