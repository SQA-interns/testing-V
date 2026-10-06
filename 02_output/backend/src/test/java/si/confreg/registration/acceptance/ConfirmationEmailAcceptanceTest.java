package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 AC3: AC-001-03. */
class ConfirmationEmailAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_03_confirmationEmailSentToParticipantWithNumberAndFee() {
    ObjectNode request = privateRegistration();
    String email = request.get("email").asString();

    JsonNode registration = completed(register(request, lastEarlyInstant()));

    Map<String, JsonNode> messages = awaitMessagesTo(email, 1);
    assertThat(messages).hasSize(1);
    String text = messages.values().iterator().next().get("Text").asString();
    assertThat(text).contains(registration.get("registrationNumber").asString());
    assertThat(text).contains(twoDecimals(amount(registration, "grossFee")));
    assertThat(text).contains(twoDecimals(amount(registration, "netFee")));
    assertThat(text).contains(twoDecimals(amount(registration, "vat")));
  }

  @Test
  void ac_001_03_companyRegistrationAlsoConfirmedToParticipant() {
    ObjectNode request = companyRegistration();
    String email = request.get("email").asString();

    JsonNode registration = completed(register(request, firstRegularInstant()));

    Map<String, JsonNode> messages = awaitMessagesTo(email, 1);
    assertThat(messages).hasSize(1);
    JsonNode message = messages.values().iterator().next();
    assertThat(message.get("To").get(0).get("Address").asString()).isEqualToIgnoringCase(email);
    String text = message.get("Text").asString();
    assertThat(text).contains(registration.get("registrationNumber").asString());
    assertThat(text).contains(twoDecimals(amount(registration, "grossFee")));
  }

  private static String twoDecimals(BigDecimal value) {
    return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
  }
}
