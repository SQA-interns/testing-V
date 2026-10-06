package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 added criteria AC-001-06 and AC-001-07. */
class RegistrationResponseAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_06_responseContainsStoredRegistrationWithSubmittedFields() {
    ObjectNode request = companyRegistration();
    request.putArray("workshops").add(workshopIds().get(0));

    HttpResponse<String> response = register(request, lastEarlyInstant());

    JsonNode registration = completed(response);
    assertThat(registration.get("registrationNumber").asString()).isNotBlank();
    for (String field :
        new String[] {
          "firstName",
          "lastName",
          "email",
          "payerType",
          "companyName",
          "companyAddress",
          "companyVatId"
        }) {
      assertThat(registration.get(field).asString())
          .as(field)
          .isEqualTo(request.get(field).asString());
    }
    assertThat(registration.get("workshop").asString()).isEqualTo(workshopIds().get(0));
  }

  @Test
  void ac_001_06_organizerReadsSameRegistration() {
    JsonNode created = completed(register(privateRegistration(), firstRegularInstant()));

    HttpResponse<String> response = getAsOrganizer(created.get("registrationNumber").asString());

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode stored = json(response);
    for (String field :
        new String[] {
          "registrationNumber",
          "firstName",
          "lastName",
          "email",
          "payerType",
          "companyName",
          "companyAddress",
          "companyVatId",
          "workshop"
        }) {
      assertThat(stored.get(field)).as(field).isEqualTo(created.get(field));
    }
    for (String field : new String[] {"netFee", "vat", "grossFee"}) {
      assertThat(amount(stored, field)).as(field).isEqualByComparingTo(amount(created, field));
    }
  }

  @Test
  void ac_001_06_registrationIsStored() {
    long before = storedRegistrations();

    completed(register(privateRegistration(), lastEarlyInstant()));

    assertThat(storedRegistrations()).isEqualTo(before + 1);
  }

  @Test
  void ac_001_07_registrationsGetDifferentNumbersEvenWithSameEmail() {
    ObjectNode first = privateRegistration();
    ObjectNode second = privateRegistration();
    second.put("email", first.get("email").asString());
    ObjectNode third = companyRegistration();

    String a = completed(register(first, lastEarlyInstant())).get("registrationNumber").asString();
    String b = completed(register(second, lastEarlyInstant())).get("registrationNumber").asString();
    String c = completed(register(third, lastEarlyInstant())).get("registrationNumber").asString();

    assertThat(a).isNotEqualTo(b).isNotEqualTo(c);
    assertThat(b).isNotEqualTo(c);
  }
}
