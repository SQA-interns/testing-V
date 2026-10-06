package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.earlyBirdTime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Registrations;

/**
 * US-001 [1]: invalid registrations are rejected with 400 and a per-field error list; nothing is
 * stored and no e-mail is sent.
 */
class ValidationAcceptanceTest extends AcceptanceTestBase {

  /** Submits the body and checks the whole rejection; returns the reported fields. */
  private List<String> assertRejected(Map<String, Object> body) {
    int before = storedCount();
    ApiClient.Response response = api().register(body, earlyBirdTime());

    assertThat(response.status()).as(response.body()).isEqualTo(400);
    assertThat(storedCount()).isEqualTo(before);
    Object email = body.get("email");
    if (email instanceof String address && !address.isBlank()) {
      assertThat(mailpit().countTo(address.trim(), SETTLE)).isZero();
    }
    return response.errorFields();
  }

  @Test
  void ac001_11_eachRequiredFieldMissingIsReported() {
    for (String field : List.of("firstName", "lastName", "email", "payerType")) {
      assertThat(assertRejected(Registrations.without(Registrations.privatePerson(), field)))
          .as(field)
          .contains(field);
    }
  }

  @Test
  void ac001_11_blankRequiredFieldsAreAllReportedTogether() {
    Map<String, Object> body = Registrations.privatePerson();
    body.put("firstName", "   ");
    body.put("lastName", "");
    body.put("email", " ");
    body.put("payerType", "");

    assertThat(assertRejected(body)).contains("firstName", "lastName", "email", "payerType");
  }

  @Test
  void ac001_12_invalidEmailAddressIsRejected() {
    for (String email : List.of("not-an-email", "ana@", "@example.org", "ana novak@example.org")) {
      assertThat(assertRejected(Registrations.with(Registrations.privatePerson(), "email", email)))
          .as(email)
          .contains("email");
    }
  }

  @Test
  void ac001_13_unknownPayerTypeIsRejected() {
    assertThat(
            assertRejected(
                Registrations.with(Registrations.privatePerson(), "payerType", "student")))
        .contains("payerType");
  }

  @Test
  void ac001_14_companyWithoutNameOrAddressIsRejected() {
    Map<String, Object> body = Registrations.company();
    body.remove("companyName");
    body.put("companyAddress", "  ");

    assertThat(assertRejected(body)).contains("companyName", "companyAddress");
  }

  @Test
  void ac001_14_companyVatIdIsOptional() {
    ApiClient.Response response =
        registerOk(Registrations.without(Registrations.company(), "companyVatId"), earlyBirdTime());

    assertThat(response.json()).containsEntry("companyVatId", null);
  }

  @Test
  void ac001_15_privatePayerWithCompanyDataIsRejected() {
    Map<String, String> companyFields = new LinkedHashMap<>();
    companyFields.put("companyName", "Primer d.o.o.");
    companyFields.put("companyAddress", "Slovenska cesta 1");
    companyFields.put("companyVatId", "SI12345678");
    companyFields.forEach(
        (field, value) ->
            assertThat(
                    assertRejected(Registrations.with(Registrations.privatePerson(), field, value)))
                .as(field)
                .contains(field));
  }

  @Test
  void ac001_15_privatePayerWithBlankCompanyFieldsIsAccepted() {
    Map<String, Object> body = Registrations.privatePerson();
    body.put("companyName", "");
    body.put("companyAddress", null);
    body.put("companyVatId", "  ");

    ApiClient.Response response = registerOk(body, earlyBirdTime());

    assertThat(response.json())
        .containsEntry("companyName", null)
        .containsEntry("companyAddress", null)
        .containsEntry("companyVatId", null);
  }

  @Test
  void ac001_17_moreThanOneWorkshopIsRejected() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.privatePerson(),
            "workshops",
            List.of(AcceptanceConfig.WORKSHOP_A, AcceptanceConfig.WORKSHOP_B));

    assertThat(assertRejected(body)).contains("workshops");
  }

  @Test
  void ac001_17_unknownWorkshopIsRejected() {
    Map<String, Object> body =
        Registrations.with(Registrations.privatePerson(), "workshops", List.of("W-UNKNOWN"));

    assertThat(assertRejected(body)).contains("workshops");
  }

  @Test
  void ac001_19_valuesLongerThanTheirLimitAreRejected() {
    Map<String, Integer> limits = new LinkedHashMap<>();
    limits.put("firstName", 100);
    limits.put("lastName", 100);
    limits.forEach(
        (field, limit) ->
            assertThat(
                    assertRejected(
                        Registrations.with(
                            Registrations.privatePerson(), field, "a".repeat(limit + 1))))
                .as(field)
                .contains(field));

    String longEmail = "a".repeat(250) + "@example.org";
    assertThat(
            assertRejected(Registrations.with(Registrations.privatePerson(), "email", longEmail)))
        .contains("email");

    Map<String, Integer> companyLimits = new LinkedHashMap<>();
    companyLimits.put("companyName", 200);
    companyLimits.put("companyAddress", 500);
    companyLimits.put("companyVatId", 30);
    companyLimits.forEach(
        (field, limit) ->
            assertThat(
                    assertRejected(
                        Registrations.with(Registrations.company(), field, "1".repeat(limit + 1))))
                .as(field)
                .contains(field));
  }

  @Test
  void ac001_19_valuesAtTheirLimitAreAccepted() {
    Map<String, Object> body = Registrations.company();
    body.put("firstName", "a".repeat(100));
    body.put("lastName", "b".repeat(100));
    body.put("companyName", "c".repeat(200));
    body.put("companyAddress", "d".repeat(500));
    body.put("companyVatId", "1".repeat(30));

    registerOk(body, earlyBirdTime());
  }
}
