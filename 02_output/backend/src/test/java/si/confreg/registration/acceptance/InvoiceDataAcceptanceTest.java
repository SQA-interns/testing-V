package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 AC4 as resolved by D-18/D-24: AC-001-04. */
class InvoiceDataAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_04_companyPayerInvoiceDataAvailableToOrganizer() {
    ObjectNode request = companyRegistration();
    JsonNode created = completed(register(request, lastEarlyInstant()));

    HttpResponse<String> response = getAsOrganizer(created.get("registrationNumber").asString());

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode stored = json(response);
    assertThat(stored.get("payerType").asString()).isEqualTo("company");
    assertThat(stored.get("companyName").asString())
        .isEqualTo(request.get("companyName").asString());
    assertThat(stored.get("companyAddress").asString())
        .isEqualTo(request.get("companyAddress").asString());
    assertThat(stored.get("companyVatId").asString())
        .isEqualTo(request.get("companyVatId").asString());
    assertThat(amount(stored, "netFee")).isEqualByComparingTo(amount(created, "netFee"));
    assertThat(amount(stored, "vat")).isEqualByComparingTo(amount(created, "vat"));
    assertThat(amount(stored, "grossFee")).isEqualByComparingTo(amount(created, "grossFee"));
  }

  @Test
  void ac_001_04_privatePayerInvoiceDataAvailableToOrganizer() {
    ObjectNode request = privateRegistration();
    JsonNode created = completed(register(request, firstRegularInstant()));

    HttpResponse<String> response = getAsOrganizer(created.get("registrationNumber").asString());

    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode stored = json(response);
    assertThat(stored.get("payerType").asString()).isEqualTo("private");
    assertThat(stored.get("firstName").asString()).isEqualTo(request.get("firstName").asString());
    assertThat(stored.get("lastName").asString()).isEqualTo(request.get("lastName").asString());
    assertThat(stored.get("email").asString()).isEqualTo(request.get("email").asString());
    assertThat(amount(stored, "grossFee")).isEqualByComparingTo(amount(created, "grossFee"));
  }

  @Test
  void ac_001_04_invoiceDataNotAvailableWithoutOrganizerCredentials() {
    JsonNode created = completed(register(companyRegistration(), lastEarlyInstant()));

    HttpResponse<String> response =
        getWithoutCredentials(created.get("registrationNumber").asString());

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.body()).doesNotContain(created.get("email").asString());
  }
}
