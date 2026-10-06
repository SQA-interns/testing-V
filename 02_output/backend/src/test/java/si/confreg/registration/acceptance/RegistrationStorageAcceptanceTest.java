package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * US-001: stored registration (AC-001-05, NFR-01), payer data for the invoice (AC-001-04, D-20) and
 * repeated registrations (AC-001-10).
 */
class RegistrationStorageAcceptanceTest extends AcceptanceTestBase {

  private static final String[] SUBMITTED_FIELDS = {
    "firstName", "lastName", "email", "payerType", "companyName", "companyAddress", "companyVatId"
  };

  private static void assertSameSubmittedValues(
      HttpResponse<String> response, Map<String, Object> body) {
    for (String field : SUBMITTED_FIELDS) {
      Object stored = json(response, "$." + field);
      assertThat(stored).as(field).isEqualTo(body.get(field));
    }
  }

  @Test
  void AC_001_05_completedRegistrationReturnsStoredRegistrationWithNewNumber() {
    Map<String, Object> body = privateRegistration(uniqueEmail("store"));

    HttpResponse<String> response = postRegistration(body, deadlineDayStart());

    assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
    assertThat((String) json(response, "$.registrationNumber")).matches(NUMBER_PATTERN);
    assertSameSubmittedValues(response, body);
    assertThat(json(response, "$.workshop")).isNull();
  }

  @Test
  void AC_001_05_organizerRetrievesTheSameRegistration() {
    Map<String, Object> body = privateRegistration(uniqueEmail("store-get"));
    HttpResponse<String> created = postRegistration(body, deadlineDayStart());
    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
    String number = (String) json(created, "$.registrationNumber");

    HttpResponse<String> read = getRegistration(number, true);

    assertThat(read.statusCode()).as(read.body()).isEqualTo(200);
    assertThat((String) json(read, "$.registrationNumber")).isEqualTo(number);
    assertSameSubmittedValues(read, body);
    for (String amount : new String[] {"netFee", "vat", "grossFee"}) {
      assertThat(amount(read, amount)).as(amount).isEqualByComparingTo(amount(created, amount));
    }
  }

  @Test
  void AC_001_05_createdResponseHasLocationOfTheRegistration() {
    HttpResponse<String> created =
        postRegistration(privateRegistration(uniqueEmail("store-loc")), deadlineDayStart());
    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);

    assertThat(created.headers().firstValue("Location"))
        .hasValue("/api/registrations/" + json(created, "$.registrationNumber"));
  }

  @Test
  void AC_001_05_registrationIsPersistedInStorage() {
    String email = uniqueEmail("store-db");

    HttpResponse<String> created = postRegistration(privateRegistration(email), deadlineDayStart());

    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
    assertThat(storedRegistrationsFor(email)).isEqualTo(1);
  }

  @Test
  void AC_001_05_registrationIsNotReadableWithoutOrganizerCredentials() {
    HttpResponse<String> created =
        postRegistration(privateRegistration(uniqueEmail("store-auth")), deadlineDayStart());
    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);

    HttpResponse<String> read =
        getRegistration((String) json(created, "$.registrationNumber"), false);

    assertThat(read.statusCode()).isEqualTo(401);
    assertThat(read.body()).doesNotContain((String) json(created, "$.email"));
  }

  @Test
  void AC_001_05_unknownRegistrationNumberIsNotFound() {
    assertThat(getRegistration("REG-999999999", true).statusCode()).isEqualTo(404);
  }

  @Test
  void AC_001_04_companyPayerDataAndAmountsForTheInvoiceAreStored() {
    Map<String, Object> body = companyRegistration(uniqueEmail("invoice"));
    HttpResponse<String> created = postRegistration(body, deadlineDayStart());
    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);

    HttpResponse<String> read =
        getRegistration((String) json(created, "$.registrationNumber"), true);

    assertThat(read.statusCode()).as(read.body()).isEqualTo(200);
    assertThat(json(read, "$.payerType")).isEqualTo("company");
    assertThat(json(read, "$.companyName")).isEqualTo(body.get("companyName"));
    assertThat(json(read, "$.companyAddress")).isEqualTo(body.get("companyAddress"));
    assertThat(json(read, "$.companyVatId")).isEqualTo(body.get("companyVatId"));
    assertThat(amount(read, "netFee")).isEqualByComparingTo(config("APP_FEE_EARLY"));
    assertThat(amount(read, "grossFee"))
        .isEqualByComparingTo(config("APP_FEE_EARLY").add(expectedVat(config("APP_FEE_EARLY"))));
  }

  @Test
  void AC_001_04_privatePayerIsStoredWithoutCompanyData() {
    HttpResponse<String> created =
        postRegistration(privateRegistration(uniqueEmail("invoice-private")), deadlineDayStart());
    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);

    HttpResponse<String> read =
        getRegistration((String) json(created, "$.registrationNumber"), true);

    assertThat(json(read, "$.payerType")).isEqualTo("private");
    assertThat(json(read, "$.companyName")).isNull();
    assertThat(json(read, "$.companyAddress")).isNull();
    assertThat(json(read, "$.companyVatId")).isNull();
  }

  @Test
  void AC_001_10_secondRegistrationWithSameEmailGetsItsOwnNumber() {
    String email = uniqueEmail("twice");

    HttpResponse<String> first = postRegistration(privateRegistration(email), deadlineDayStart());
    HttpResponse<String> second = postRegistration(privateRegistration(email), deadlineDayStart());

    assertThat(first.statusCode()).as(first.body()).isEqualTo(201);
    assertThat(second.statusCode()).as(second.body()).isEqualTo(201);
    assertThat((String) json(second, "$.registrationNumber"))
        .isNotEqualTo(json(first, "$.registrationNumber"));
    assertThat(storedRegistrationsFor(email)).isEqualTo(2);
  }
}
