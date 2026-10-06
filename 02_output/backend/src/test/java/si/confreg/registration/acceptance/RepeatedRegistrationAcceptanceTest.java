package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** AC-001-09: a repeated registration is stored separately; the first stays unchanged. */
class RepeatedRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-09 second registration with the same e-mail gets its own number and fee")
  void ac001_09_secondRegistrationStoredSeparately() {
    ApiResponse first = registerSuccessfully(anaPrivate(), daysFromDeadline(-11));
    String firstNumber = registrationNumberOf(first);
    JsonNode firstBefore = storedRegistration(firstNumber).json();

    ApiResponse second = registerSuccessfully(anaPrivate(), daysFromDeadline(5));
    String secondNumber = registrationNumberOf(second);

    assertThat(secondNumber).isNotEqualTo(firstNumber);
    assertFee(storedRegistration(firstNumber), earlyFee());
    assertFee(storedRegistration(secondNumber), regularFee());
    assertThat(storedRegistration(firstNumber).json())
        .as("first registration unchanged")
        .isEqualTo(firstBefore);
  }

  @Test
  @DisplayName("AC-001-09 each registration gets its own confirmation (Q3)")
  void ac001_09_eachRegistrationConfirmed() {
    String first = registrationNumberOf(registerSuccessfully(anaPrivate(), daysFromDeadline(-11)));
    String second = registrationNumberOf(registerSuccessfully(anaPrivate(), daysFromDeadline(5)));

    var messages = awaitMessagesTo("ana.novak@example.org", 2);

    assertThat(messages).hasSize(2);
    String subjects =
        messages.get(0).path("Subject").asString("") + messages.get(1).path("Subject").asString("");
    String texts =
        message(messages.get(0)).path("Text").asString("")
            + message(messages.get(1)).path("Text").asString("");
    assertThat(subjects + texts).contains(first).contains(second);
  }
}
