package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** AC-001-10: the organizer reads a stored registration with HTTP Basic; others cannot. */
class OrganizerAccessAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> FIXED_FIELDS =
      List.of(
          "registrationNumber",
          "firstName",
          "lastName",
          "email",
          "payerType",
          "companyName",
          "companyAddress",
          "companyVatId",
          "workshop",
          "netFee",
          "vat",
          "grossFee");

  @Test
  @DisplayName("AC-001-10 organizer reads the stored registration with every fixed field")
  void ac001_10_organizerReadsStoredRegistration() {
    String workshop = workshopIds().get(0);
    Map<String, Object> body = anaForSlovenianCompany();
    body.put("workshops", List.of(workshop));
    String number = registrationNumberOf(registerSuccessfully(body, daysFromDeadline(-11)));

    ApiResponse response = storedRegistration(number);
    JsonNode stored = response.json();

    for (String field : FIXED_FIELDS) {
      assertThat(stored.has(field)).as("field %s", field).isTrue();
    }
    assertThat(stored.path("registrationNumber").asString("")).isEqualTo(number);
    assertThat(stored.path("firstName").asString("")).isEqualTo("Ana");
    assertThat(stored.path("lastName").asString("")).isEqualTo("Novak");
    assertThat(stored.path("email").asString("")).isEqualTo("ana.novak@example.org");
    assertThat(stored.path("payerType").asString("")).isEqualTo("company");
    assertThat(stored.path("companyVatId").asString("")).isEqualTo("SI00000001");
    assertThat(stored.path("workshop").asString("")).isEqualTo(workshop);
    assertFee(response, earlyFee());
  }

  @Test
  @DisplayName("AC-001-10 request without credentials gets 401 and no registration data")
  void ac001_10_withoutCredentialsIs401() {
    String number = registrationNumberOf(registerSuccessfully(anaPrivate(), daysFromDeadline(-3)));

    ApiResponse response = getRegistration(number, null, null);

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body()).doesNotContain("ana.novak@example.org").doesNotContain("Novak");
  }

  @Test
  @DisplayName("AC-001-10 request with a wrong password gets 401 and no registration data")
  void ac001_10_wrongPasswordIs401() {
    String number = registrationNumberOf(registerSuccessfully(anaPrivate(), daysFromDeadline(-3)));

    ApiResponse response = getRegistration(number, ORGANIZER_USERNAME, ORGANIZER_PASSWORD + "x");

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body()).doesNotContain("ana.novak@example.org").doesNotContain("Novak");
  }

  @Test
  @DisplayName("AC-001-10 request with a wrong user name gets 401")
  void ac001_10_wrongUserIs401() {
    String number = registrationNumberOf(registerSuccessfully(anaPrivate(), daysFromDeadline(-3)));

    ApiResponse response = getRegistration(number, ORGANIZER_USERNAME + "x", ORGANIZER_PASSWORD);

    assertThat(response.status()).isEqualTo(401);
  }

  @Test
  @DisplayName("AC-001-10 unknown registration number gets 404 for the organizer")
  void ac001_10_unknownRegistrationIs404() {
    ApiResponse response = getRegistrationAsOrganizer("REG-0000000000");

    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(404);
  }
}
