package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Confirmation response and e-mail (US-001: AC-001-04). */
class ConfirmationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_04_storedRegistrationAnswers201WithRegistrationNumber() {
    ApiResponse response = postRegistration(privatePayer(uniqueEmail()), earlyInstant());

    assertThat(response.status()).isEqualTo(201);
    JsonNode body = response.json();
    assertThat(body.get("registrationNumber")).isNotNull();
    assertThat(body.get("registrationNumber").asString()).isNotBlank();
  }

  @Test
  void ac_001_04_exactlyOneConfirmationMailStatesNumberAndFees() {
    String email = uniqueEmail();
    JsonNode created = register(privatePayer(email), regularInstant());
    String number = created.get("registrationNumber").asString();
    BigDecimal net = feeRegular();
    BigDecimal vat = vatOf(net, vatRate());

    List<JsonNode> mails = awaitMailsTo(email);

    assertThat(mails).hasSize(1);
    String text = mailText(mails.get(0));
    assertThat(text)
        .contains(number)
        .contains(net.setScale(2).toPlainString())
        .contains(vat.setScale(2).toPlainString())
        .contains(net.add(vat).setScale(2).toPlainString());
  }

  @Test
  void ac_001_04_eachOfTwoRegistrationsGetsItsOwnSingleConfirmation() {
    String first = uniqueEmail();
    String second = uniqueEmail();
    register(privatePayer(first), earlyInstant());
    register(sloveneCompany(second), earlyInstant());

    assertThat(awaitMailsTo(first)).hasSize(1);
    assertThat(awaitMailsTo(second)).hasSize(1);
  }
}
