package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 added validation criteria AC-001-08 to AC-001-13 (D-21). */
class ValidationAcceptanceTest extends AcceptanceTestBase {

  @ParameterizedTest
  @ValueSource(strings = {"firstName", "lastName", "email", "payerType"})
  void ac_001_08_missingRequiredFieldRejected(String field) {
    ObjectNode body = privateRegistration();
    body.remove(field);

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"firstName", "lastName", "email", "payerType"})
  void ac_001_08_blankRequiredFieldRejected(String field) {
    ObjectNode body = privateRegistration();
    body.put(field, "   ");

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plainaddress",
        "missing-at.example.com",
        "@example.com",
        "user@",
        "user@@example.com",
        "user name@example.com",
        "user@exam ple.com"
      })
  void ac_001_09_invalidEmailRejected(String email) {
    ObjectNode body = privateRegistration();
    body.put("email", email);

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"other", "business", "PRIVATE"})
  void ac_001_10_unknownPayerTypeRejected(String payerType) {
    ObjectNode body = privateRegistration();
    body.put("payerType", payerType);

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"companyName", "companyAddress", "companyVatId"})
  void ac_001_11_companyPayerWithoutCompanyFieldRejected(String field) {
    ObjectNode body = companyRegistration();
    body.remove(field);

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"companyName", "companyAddress", "companyVatId"})
  void ac_001_11_companyPayerWithBlankCompanyFieldRejected(String field) {
    ObjectNode body = companyRegistration();
    body.put(field, " ");

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(strings = {"companyName", "companyAddress", "companyVatId"})
  void ac_001_12_privatePayerWithCompanyFieldRejected(String field) {
    ObjectNode body = privateRegistration();
    body.put(field, companyRegistration().get(field).asString());

    assertRejected(body);
  }

  @Test
  void ac_001_12_privatePayerWithBlankCompanyFieldsAccepted() {
    ObjectNode body = privateRegistration();
    body.put("companyName", "");
    body.putNull("companyAddress");

    JsonNode registration = completed(register(body, lastEarlyInstant()));

    assertThat(registration.get("companyName").isNull()).isTrue();
    assertThat(registration.get("companyAddress").isNull()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "firstName:100",
        "lastName:100",
        "companyName:200",
        "companyAddress:500",
        "companyVatId:30"
      })
  void ac_001_13_fieldLongerThanLimitRejected(String fieldAndLimit) {
    String field = fieldAndLimit.substring(0, fieldAndLimit.indexOf(':'));
    int limit = Integer.parseInt(fieldAndLimit.substring(fieldAndLimit.indexOf(':') + 1));
    ObjectNode body = companyRegistration();
    body.put(field, "x".repeat(limit + 1));

    assertRejected(body);
  }

  @Test
  void ac_001_13_emailLongerThanLimitRejected() {
    ObjectNode body = privateRegistration();
    String domain = "@example.com";
    body.put("email", "a".repeat(255 - domain.length()) + domain);

    assertRejected(body);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "firstName:100",
        "lastName:100",
        "companyName:200",
        "companyAddress:500",
        "companyVatId:30"
      })
  void ac_001_13_fieldAtLimitAccepted(String fieldAndLimit) {
    String field = fieldAndLimit.substring(0, fieldAndLimit.indexOf(':'));
    int limit = Integer.parseInt(fieldAndLimit.substring(fieldAndLimit.indexOf(':') + 1));
    ObjectNode body = companyRegistration();
    body.put(field, "x".repeat(limit));

    JsonNode registration = completed(register(body, lastEarlyInstant()));

    assertThat(registration.get(field).asString()).hasSize(limit);
  }
}
