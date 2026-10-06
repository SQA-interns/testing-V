package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Rejection of missing or invalid fields (US-001: AC-001-07; D-10). API level. */
class ValidationAcceptanceTest extends AcceptanceTestBase {

  /** Posts the body and asserts 422, exactly one error per expected field, no storage, no mail. */
  private void assertRejected(Map<String, Object> body, String... expectedFields) {
    ApiResponse response = postRegistration(body, earlyInstant());

    assertThat(response.status()).as("status of %s", response.body()).isEqualTo(422);
    List<String> fields = new ArrayList<>();
    JsonNode errors = response.json().get("errors");
    assertThat(errors).as("errors array").isNotNull();
    errors.forEach(error -> fields.add(error.get("field").asString()));
    assertThat(fields).as("one error per field").doesNotHaveDuplicates();
    assertThat(fields).containsExactlyInAnyOrder(expectedFields);
    errors.forEach(error -> assertThat(error.get("message").asString()).isNotBlank());

    Object email = body.get("email");
    if (email instanceof String address && !address.isBlank()) {
      assertThat(storedRegistrationsFor(address)).as("nothing stored").isZero();
      assertNoMailTo(address);
    }
  }

  @Test
  void ac_001_07_missingFirstNameIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.remove("firstName");
    assertRejected(body, "firstName");
  }

  @Test
  void ac_001_07_blankLastNameIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.put("lastName", "   ");
    assertRejected(body, "lastName");
  }

  @Test
  void ac_001_07_missingEmailIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.remove("email");
    assertRejected(body, "email");
  }

  @Test
  void ac_001_07_invalidEmailIsRejected() {
    String invalid = "ana.novak+" + System.nanoTime() + "@example";
    assertRejected(privatePayer(invalid), "email");
  }

  @Test
  void ac_001_07_emailWithLineBreakIsRejected() {
    String injected = uniqueEmail() + "\r\nBcc: someone@example.org";
    assertRejected(privatePayer(injected), "email");
  }

  @Test
  void ac_001_07_missingPayerTypeIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.remove("payerType");
    assertRejected(body, "payerType");
  }

  @Test
  void ac_001_07_unknownPayerTypeIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.put("payerType", "student");
    assertRejected(body, "payerType");
  }

  @Test
  void ac_001_07_companyPayerWithoutCompanyDataIsRejectedPerField() {
    Map<String, Object> body = sloveneCompany(uniqueEmail());
    body.remove("companyName");
    body.put("companyAddress", "");
    body.remove("companyVatId");
    assertRejected(body, "companyName", "companyAddress", "companyVatId");
  }

  @Test
  void ac_001_07_companyPayerWithoutVatIdIsRejected() {
    Map<String, Object> body = sloveneCompany(uniqueEmail());
    body.remove("companyVatId");
    assertRejected(body, "companyVatId");
  }

  @Test
  void ac_001_07_moreThanOneWorkshopIsRejected() {
    List<String> ids = new ArrayList<>(workshops().keySet());
    assertThat(ids).as("at least two configured workshops").hasSizeGreaterThanOrEqualTo(2);
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.put("workshops", ids.subList(0, 2));
    assertRejected(body, "workshops");
  }

  @Test
  void ac_001_07_unknownWorkshopIsRejected() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.put("workshops", List.of("not-a-configured-workshop"));
    assertRejected(body, "workshops");
  }

  @Test
  void ac_001_07_severalInvalidFieldsGiveOneErrorEach() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.remove("firstName");
    body.put("lastName", "");
    body.put("payerType", "nobody");
    assertRejected(body, "firstName", "lastName", "payerType");
  }

  @Test
  void ac_001_07_emptyObjectIsRejectedForEveryRequiredParticipantField() {
    ApiResponse response = postRegistration(Map.of(), earlyInstant());

    assertThat(response.status()).as("status of %s", response.body()).isEqualTo(422);
    List<String> fields = new ArrayList<>();
    response.json().get("errors").forEach(error -> fields.add(error.get("field").asString()));
    assertThat(fields).containsExactlyInAnyOrder("firstName", "lastName", "email", "payerType");
  }
}
