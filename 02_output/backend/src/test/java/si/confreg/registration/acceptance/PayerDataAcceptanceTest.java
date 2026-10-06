package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** AC-001-05: company data stored for company payers, none for private payers, no invoice. */
class PayerDataAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-05 company payer: name, address and VAT ID are stored")
  void ac001_05_companyDataStored() {
    ApiResponse created = registerSuccessfully(anaForSlovenianCompany(), daysFromDeadline(-3));
    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();

    assertThat(stored.path("payerType").asString("")).isEqualTo("company");
    assertThat(stored.path("companyName").asString("")).isEqualTo("Primer d.o.o.");
    assertThat(stored.path("companyAddress").asString(""))
        .isEqualTo("Koroška cesta 1, 2000 Maribor");
    assertThat(stored.path("companyVatId").asString("")).isEqualTo("SI00000001");
  }

  @Test
  @DisplayName("AC-001-05 private payer: stored without company data")
  void ac001_05_privatePayerStoredWithoutCompanyData() {
    ApiResponse created = registerSuccessfully(anaPrivate(), daysFromDeadline(-3));
    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();

    assertThat(stored.path("payerType").asString("")).isEqualTo("private");
    assertNoCompanyData(stored);
  }

  @Test
  @DisplayName("AC-001-05 private payer: company fields sent anyway are not stored (D-08)")
  void ac001_05_companyFieldsIgnoredForPrivatePayer() {
    Map<String, Object> body = anaForSlovenianCompany();
    body.put("payerType", "private");

    ApiResponse created = registerSuccessfully(body, daysFromDeadline(-3));

    assertNoCompanyData(created.json());
    assertNoCompanyData(storedRegistration(registrationNumberOf(created)).json());
  }

  @Test
  @DisplayName("AC-001-05 no invoice and no payment request are created")
  void ac001_05_noInvoiceAndNoPaymentRequest() {
    ApiResponse created = registerSuccessfully(anaForSlovenianCompany(), daysFromDeadline(-3));
    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();

    assertNoInvoiceOrPaymentField(created.json());
    assertNoInvoiceOrPaymentField(stored);
    assertThat(awaitMessagesTo("ana.novak@example.org", 1)).as("only the confirmation").hasSize(1);
    assertThat(totalMessagesAfterSettle()).as("no other e-mail (invoice, payment)").isEqualTo(1);
    for (String path : new String[] {"/api/invoices", "/api/payments"}) {
      ApiResponse response = getAsOrganizer(path);
      assertThat(response.status()).as("organizer GET %s", path).isEqualTo(404);
    }
  }

  private static void assertNoCompanyData(JsonNode registration) {
    for (String field : new String[] {"companyName", "companyAddress", "companyVatId"}) {
      assertThat(registration.has(field)).as("%s present in the registration", field).isTrue();
      assertThat(registration.path(field).isNull()).as("%s is null", field).isTrue();
    }
  }

  private static void assertNoInvoiceOrPaymentField(JsonNode registration) {
    for (String name : registration.propertyNames()) {
      assertThat(name.toLowerCase(Locale.ROOT)).doesNotContain("invoice").doesNotContain("payment");
    }
  }
}
