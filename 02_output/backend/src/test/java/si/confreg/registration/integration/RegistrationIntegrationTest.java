package si.confreg.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;

/** NFR-01, SR-01, SR-02, SB-02, SB-07, SB-10 and ES-09 against the running backend. */
@ExtendWith(OutputCaptureExtension.class)
class RegistrationIntegrationTest extends IntegrationTestBase {

  private static String body(String firstName, String lastName, String email) {
    return """
        {"firstName":"%s","lastName":"%s","email":"%s","payerType":"company",
         "companyName":"Čebelarstvo Žužek d.o.o.","companyAddress":"Šmartinska cesta 1\\nLjubljana",
         "companyVatId":"SI12345678","workshops":["I1"]}
        """
        .formatted(firstName, lastName, email);
  }

  private static String uniqueEmail() {
    return "it-" + UUID.randomUUID() + "@example.com";
  }

  @Test
  void slovenianCharactersSurviveStorageAndEmail() throws Exception {
    String email = uniqueEmail();

    HttpResponse<String> created = postJson(body("Čedomir Đurđa", "Šuštaršič-Žagar", email));

    assertThat(created.statusCode()).isEqualTo(201);
    String number = json(created).get("registrationNumber").asString();
    JsonNode stored = json(get("/api/registrations/" + number, USERNAME, PASSWORD));
    assertThat(stored.get("firstName").asString()).isEqualTo("Čedomir Đurđa");
    assertThat(stored.get("lastName").asString()).isEqualTo("Šuštaršič-Žagar");
    assertThat(stored.get("companyName").asString()).isEqualTo("Čebelarstvo Žužek d.o.o.");
    assertThat(stored.get("companyAddress").asString()).isEqualTo("Šmartinska cesta 1\nLjubljana");
    String text = mailTextTo(email);
    assertThat(text)
        .contains("Čedomir Đurđa Šuštaršič-Žagar")
        .contains("Čebelarstvo Žužek d.o.o.")
        .contains("Šmartinska cesta 1");
  }

  @Test
  void locationHeaderPointsToStoredRegistration() {
    HttpResponse<String> created = postJson(body("Ana", "Kovač", uniqueEmail()));

    String number = json(created).get("registrationNumber").asString();
    assertThat(created.headers().firstValue("Location")).contains("/api/registrations/" + number);
    assertThat(number).matches("REG-\\d{6,}");
  }

  @Test
  void personalDataNeverWrittenToLogs(CapturedOutput output) {
    String email = uniqueEmail();
    String marker = "Logcheck" + UUID.randomUUID().toString().substring(0, 8);

    postJson(body(marker, marker, email));
    postJson(body(marker, marker, "not-an-email-" + marker));

    assertThat(output.getAll()).doesNotContain(marker).doesNotContain(email);
  }

  @Test
  void securityHeadersOnApiResponses() {
    HttpResponse<String> response = postJson(body("Ana", "Kovač", uniqueEmail()));

    assertThat(response.headers().firstValue("Content-Security-Policy"))
        .contains("default-src 'none'; frame-ancestors 'none'");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    assertThat(response.headers().firstValue("X-Frame-Options")).contains("DENY");
    assertThat(response.headers().firstValue("Referrer-Policy")).contains("no-referrer");
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
  }

  @Test
  void malformedUnknownAndWrongTypeBodiesAreBadRequests() {
    HttpResponse<String> malformed = postJson("{\"firstName\":");
    HttpResponse<String> unknown =
        postJson(body("Ana", "Kovač", uniqueEmail()).replace("{", "{\"admin\":true,"));
    HttpResponse<String> wrongType =
        postJson(body("Ana", "Kovač", uniqueEmail()).replace("[\"I1\"]", "{\"id\":1}"));

    for (HttpResponse<String> response : new HttpResponse[] {malformed, unknown, wrongType}) {
      assertThat(response.statusCode()).isEqualTo(400);
      assertThat(response.headers().firstValue("Content-Type").orElse(""))
          .startsWith("application/problem+json");
      assertThat(response.body()).doesNotContain("Exception").doesNotContain("tools.jackson");
    }
  }

  @Test
  void nonJsonBodyIsUnsupportedMediaType() {
    HttpResponse<String> response = post("/api/registrations", "text/plain", "hello");

    assertThat(response.statusCode()).isEqualTo(415);
  }

  @Test
  void oversizedBodyIsRefused() {
    String padding = "x".repeat(17 * 1024);

    HttpResponse<String> response = postJson(body(padding, "B", uniqueEmail()));

    assertThat(response.statusCode()).isEqualTo(413);
  }

  @Test
  void organizerEndpointRequiresValidCredentials() {
    HttpResponse<String> anonymous = get("/api/registrations/REG-000001", null, null);
    HttpResponse<String> wrong = get("/api/registrations/REG-000001", USERNAME, "wrong-password");
    HttpResponse<String> missing = get("/api/registrations/REG-999999", USERNAME, PASSWORD);

    assertThat(anonymous.statusCode()).isEqualTo(401);
    assertThat(anonymous.headers().firstValue("WWW-Authenticate").orElse("")).startsWith("Basic");
    assertThat(wrong.statusCode()).isEqualTo(401);
    assertThat(missing.statusCode()).isEqualTo(404);
  }

  @Test
  void otherEndpointsNeedOrganizerAndUnknownOnesAreNotFound() {
    assertThat(get("/actuator/env", null, null).statusCode()).isEqualTo(401);
    assertThat(get("/api/anything", null, null).statusCode()).isEqualTo(401);
    assertThat(get("/actuator/env", USERNAME, PASSWORD).statusCode()).isEqualTo(404);
    assertThat(post("/api/registrations/REG-000001", "application/json", "{}").statusCode())
        .isEqualTo(401);
  }

  @Test
  void healthProbesArePublicWithoutDetails() {
    for (String path :
        new String[] {
          "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
        }) {
      HttpResponse<String> response = get(path, null, null);
      assertThat(response.statusCode()).as(path).isEqualTo(200);
      JsonNode health = json(response);
      assertThat(health.get("status").asString()).isEqualTo("UP");
      assertThat(health.has("components")).isFalse();
      assertThat(health.has("details")).isFalse();
    }
  }

  @Test
  void invalidTestClockHeaderIsBadRequest() {
    HttpResponse<String> response =
        send(
            java.net.http.HttpRequest.newBuilder(uri("/api/registrations"))
                .header("Content-Type", "application/json")
                .header("X-Test-Now", "tomorrow")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                .build());

    assertThat(response.statusCode()).isEqualTo(400);
  }
}
