package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Organizer reads registrations (US-001: AC-001-10, AC-001-11, AC-001-12). */
class OrganizerAccessAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_10_organizerReadsEveryFieldOfStoredRegistration() {
    String email = uniqueEmail();
    Map<String, Object> body = sloveneCompany(email);
    String workshopId = workshops().keySet().iterator().next();
    body.put("workshops", List.of(workshopId));
    JsonNode created = register(body, earlyInstant());
    String number = created.get("registrationNumber").asString();

    ApiResponse response = getAsOrganizer(number);

    assertThat(response.status()).isEqualTo(200);
    JsonNode stored = response.json();
    assertThat(stored.get("registrationNumber").asString()).isEqualTo(number);
    assertThat(stored.get("firstName").asString()).isEqualTo("Ana");
    assertThat(stored.get("lastName").asString()).isEqualTo("Novak");
    assertThat(stored.get("email").asString()).isEqualTo(email);
    assertThat(stored.get("payerType").asString()).isEqualTo("company");
    assertThat(stored.get("companyName").asString()).isEqualTo("Primer d.o.o.");
    assertThat(stored.get("companyAddress").asString()).isEqualTo("Koroška cesta 1, 2000 Maribor");
    assertThat(stored.get("companyVatId").asString()).isEqualTo("SI00000001");
    assertThat(stored.get("workshop").asString()).isEqualTo(workshopId);
    BigDecimal net = feeEarly();
    BigDecimal vat = vatOf(net, vatRate());
    assertAmounts(stored, net, vat, net.add(vat));
  }

  @Test
  void ac_001_11_readingWithoutCredentialsIsRejectedWithoutData() {
    String email = uniqueEmail();
    JsonNode created = register(privatePayer(email), earlyInstant());

    ApiResponse response =
        getRegistration(created.get("registrationNumber").asString(), null, null);

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body()).doesNotContain(email).doesNotContain("Novak");
  }

  @Test
  void ac_001_11_readingWithWrongPasswordIsRejectedWithoutData() {
    String email = uniqueEmail();
    JsonNode created = register(privatePayer(email), earlyInstant());

    ApiResponse response =
        getRegistration(
            created.get("registrationNumber").asString(),
            ORGANIZER_USERNAME,
            ORGANIZER_PASSWORD + "-wrong");

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body()).doesNotContain(email).doesNotContain("Novak");
  }

  @Test
  void ac_001_11_readingWithWrongUsernameIsRejectedWithoutData() {
    String email = uniqueEmail();
    JsonNode created = register(privatePayer(email), earlyInstant());

    ApiResponse response =
        getRegistration(
            created.get("registrationNumber").asString(), "not-the-organizer", ORGANIZER_PASSWORD);

    assertThat(response.status()).isEqualTo(401);
    assertThat(response.body()).doesNotContain(email).doesNotContain("Novak");
  }

  @Test
  void ac_001_12_unknownRegistrationNumberAnswers404WithoutData() {
    register(privatePayer(uniqueEmail()), earlyInstant());

    ApiResponse response = getAsOrganizer("CR-DOESNOTEXIST");

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).doesNotContain("@example.org").doesNotContain("Novak");
  }
}
