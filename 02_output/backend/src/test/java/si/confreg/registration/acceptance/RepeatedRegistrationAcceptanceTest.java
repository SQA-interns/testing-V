package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Repeated registration with the same e-mail (US-001: AC-001-09, OQ-02). */
class RepeatedRegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_09_secondRegistrationIsStoredWithOwnNumberAndFeeFirstUnchanged() {
    String email = uniqueEmail();
    JsonNode first = register(privatePayer(email), earlyInstant());
    String firstNumber = first.get("registrationNumber").asString();
    ApiResponse firstBefore = getAsOrganizer(firstNumber);
    assertThat(firstBefore.status()).isEqualTo(200);

    JsonNode second = register(privatePayer(email), regularInstant());
    String secondNumber = second.get("registrationNumber").asString();

    assertThat(secondNumber).isNotEqualTo(firstNumber);
    assertThat(amount(first, "netFee")).isEqualByComparingTo(feeEarly());
    assertThat(amount(second, "netFee")).isEqualByComparingTo(feeRegular());
    ApiResponse firstAfter = getAsOrganizer(firstNumber);
    assertThat(firstAfter.status()).isEqualTo(200);
    assertThat(firstAfter.json()).isEqualTo(firstBefore.json());
    ApiResponse secondStored = getAsOrganizer(secondNumber);
    assertThat(secondStored.status()).isEqualTo(200);
    assertThat(amount(secondStored.json(), "netFee")).isEqualByComparingTo(feeRegular());
    assertThat(storedRegistrationsFor(email)).isEqualTo(2);
  }

  @Test
  void ac_001_09_eachRepeatedRegistrationGetsItsOwnConfirmation() {
    String email = uniqueEmail();
    JsonNode first = register(privatePayer(email), earlyInstant());
    JsonNode second = register(privatePayer(email), earlyInstant());

    assertThat(second.get("registrationNumber").asString())
        .isNotEqualTo(first.get("registrationNumber").asString());
    assertThat(awaitMailsTo(email)).hasSize(2);
  }
}
