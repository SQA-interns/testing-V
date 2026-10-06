package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Payer data stored, no invoice or payment request (US-001: AC-001-05). */
class PayerDataAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_05_companyPayerDataIsStoredWithRegistration() {
    JsonNode created = register(sloveneCompany(uniqueEmail()), earlyInstant());

    JsonNode stored = getAsOrganizer(created.get("registrationNumber").asString()).json();
    assertThat(stored.get("payerType").asString()).isEqualTo("company");
    assertThat(stored.get("companyName").asString()).isEqualTo("Primer d.o.o.");
    assertThat(stored.get("companyAddress").asString()).isEqualTo("Koroška cesta 1, 2000 Maribor");
    assertThat(stored.get("companyVatId").asString()).isEqualTo("SI00000001");
  }

  @Test
  void ac_001_05_privatePayerIsStoredWithoutCompanyData() {
    JsonNode created = register(privatePayer(uniqueEmail()), earlyInstant());

    JsonNode stored = getAsOrganizer(created.get("registrationNumber").asString()).json();
    assertThat(stored.get("payerType").asString()).isEqualTo("private");
    assertThat(stored.get("companyName").isNull()).as("companyName is null").isTrue();
    assertThat(stored.get("companyAddress").isNull()).as("companyAddress is null").isTrue();
    assertThat(stored.get("companyVatId").isNull()).as("companyVatId is null").isTrue();
  }

  @Test
  void ac_001_05_noInvoiceAndNoPaymentRequestIsCreated() {
    String companyEmail = uniqueEmail();
    String privateEmail = uniqueEmail();
    register(sloveneCompany(companyEmail), earlyInstant());
    register(privatePayer(privateEmail), earlyInstant());

    // the only message per registration is the confirmation (no payment request mail)
    List<JsonNode> companyMails = awaitMailsTo(companyEmail);
    List<JsonNode> privateMails = awaitMailsTo(privateEmail);
    assertThat(companyMails).hasSize(1);
    assertThat(privateMails).hasSize(1);
    assertThat(mailText(companyMails.get(0)).toLowerCase())
        .doesNotContain("invoice no")
        .doesNotContain("payment request");
    // storage holds registrations only: no invoice or payment tables (storage contract)
    assertThat(tablesInPublicSchema())
        .allSatisfy(
            table ->
                assertThat(table)
                    .doesNotContainIgnoringCase("invoice")
                    .doesNotContainIgnoringCase("payment"));
  }
}
