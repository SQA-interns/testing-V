package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** AC-001-04 (API level): 201 with registration number and exactly one confirmation e-mail. */
class ConfirmationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-04 API answers 201 with the registration number of the stored registration")
  void ac001_04_created201WithRegistrationNumber() {
    ApiResponse response = register(anaPrivate(), daysFromDeadline(-11));

    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(201);
    String number = registrationNumberOf(response);
    assertThat(number).isNotBlank();
    assertThat(registrationNumberOf(storedRegistration(number))).isEqualTo(number);
  }

  @Test
  @DisplayName("AC-001-04 exactly one confirmation e-mail with number, net fee, VAT and gross fee")
  void ac001_04_exactlyOneConfirmationEmailWithAmounts() {
    ApiResponse response = registerSuccessfully(anaPrivate(), daysFromDeadline(-11));
    String number = registrationNumberOf(response);
    BigDecimal net = earlyFee();
    BigDecimal vat = vatOf(net);

    List<JsonNode> messages = awaitMessagesTo("ana.novak@example.org", 1);

    assertThat(messages).as("confirmation e-mails to the participant").hasSize(1);
    assertThat(totalMessagesAfterSettle()).as("all e-mails sent").isEqualTo(1);
    JsonNode mail = message(messages.get(0));
    String text = mail.path("Text").asString("");
    assertThat(mail.path("Subject").asString("") + "\n" + text).contains(number);
    assertThat(text)
        .contains(net.toPlainString())
        .contains(vat.toPlainString())
        .contains(net.add(vat).toPlainString());
  }

  @Test
  @DisplayName("AC-001-04 confirmation for a company payer goes to the participant")
  void ac001_04_confirmationGoesToParticipantForCompanyPayer() {
    registerSuccessfully(anaForSlovenianCompany(), daysFromDeadline(5));

    assertThat(awaitMessagesTo("ana.novak@example.org", 1)).hasSize(1);
    assertThat(totalMessagesAfterSettle()).isEqualTo(1);
  }

  @Test
  @DisplayName("AC-001-04 NFR-01 Slovenian characters survive storage and e-mail unchanged")
  void ac001_04_slovenianCharactersSurviveStorageAndEmail() {
    Map<String, Object> body = privatePayer("Čedomir", "Šušteršič Žnidaršič", "cedo@example.org");
    body.put("payerType", "company");
    body.put("companyName", "Žabjak d.o.o.");
    body.put("companyAddress", "Čopova ulica 5, 1000 Ljubljana");
    body.put("companyVatId", "SI00000001");

    ApiResponse created = registerSuccessfully(body, daysFromDeadline(-1));
    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();

    assertThat(stored.path("firstName").asString("")).isEqualTo("Čedomir");
    assertThat(stored.path("lastName").asString("")).isEqualTo("Šušteršič Žnidaršič");
    assertThat(stored.path("companyName").asString("")).isEqualTo("Žabjak d.o.o.");
    assertThat(stored.path("companyAddress").asString(""))
        .isEqualTo("Čopova ulica 5, 1000 Ljubljana");
    List<JsonNode> messages = awaitMessagesTo("cedo@example.org", 1);
    assertThat(messages).hasSize(1);
    assertThat(message(messages.get(0)).path("Text").asString(""))
        .contains("Čedomir")
        .contains("Šušteršič Žnidaršič");
  }
}
