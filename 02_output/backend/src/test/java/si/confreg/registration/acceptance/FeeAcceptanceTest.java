package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 fee criteria AC-001-01, AC-001-02, AC-001-05 and AC-001-17. */
class FeeAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_01_earlyFeeOnLastInstantOfDeadlineDay() {
    JsonNode registration = completed(register(privateRegistration(), lastEarlyInstant()));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeEarly());
  }

  @Test
  void ac_001_01_earlyFeeWellBeforeDeadline() {
    Instant monthBefore = deadlineEnd().minus(Duration.ofDays(30));

    JsonNode registration = completed(register(privateRegistration(), monthBefore));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeEarly());
  }

  @Test
  void ac_001_01_earlyFeeAtStartOfDeadlineDayInConferenceZone() {
    Instant startOfDeadlineDay = deadlineEnd().atZone(conferenceZone()).minusDays(1).toInstant();

    JsonNode registration = completed(register(companyRegistration(), startOfDeadlineDay));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeEarly());
  }

  @Test
  void ac_001_02_regularFeeOnFirstInstantAfterDeadline() {
    JsonNode registration = completed(register(privateRegistration(), firstRegularInstant()));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeRegular());
  }

  @Test
  void ac_001_02_regularFeeWellAfterDeadline() {
    Instant monthAfter = deadlineEnd().plus(Duration.ofDays(30));

    JsonNode registration = completed(register(companyRegistration(), monthAfter));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeRegular());
  }

  @Test
  void ac_001_02_deadlineIsInterpretedInConferenceZoneNotUtc() {
    // The UTC calendar day of this instant is still the deadline day, but in the conference zone
    // (east of UTC) the next day has already started.
    Instant justAfterLocalMidnight = firstRegularInstant().plusSeconds(60);

    JsonNode registration = completed(register(privateRegistration(), justAfterLocalMidnight));

    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(feeRegular());
  }

  @Test
  void ac_001_05_earlyAmountsSplitIntoNetAndVat() {
    JsonNode registration = completed(register(privateRegistration(), lastEarlyInstant()));

    assertAmounts(registration, feeEarly());
  }

  @Test
  void ac_001_05_regularAmountsSplitIntoNetAndVat() {
    JsonNode registration = completed(register(companyRegistration(), firstRegularInstant()));

    assertAmounts(registration, feeRegular());
  }

  @Test
  void ac_001_17_workshopDoesNotChangeFee() {
    Instant now = lastEarlyInstant();
    ObjectNode withWorkshop = privateRegistration();
    withWorkshop.putArray("workshops").add(workshopIds().get(0));

    JsonNode without = completed(register(privateRegistration(), now));
    JsonNode with = completed(register(withWorkshop, now));

    assertThat(amount(with, "grossFee")).isEqualByComparingTo(amount(without, "grossFee"));
    assertThat(amount(with, "netFee")).isEqualByComparingTo(amount(without, "netFee"));
    assertThat(amount(with, "vat")).isEqualByComparingTo(amount(without, "vat"));
  }

  private void assertAmounts(JsonNode registration, BigDecimal gross) {
    BigDecimal net = expectedNet(gross, vatRate());
    assertThat(amount(registration, "grossFee")).isEqualByComparingTo(gross);
    assertThat(amount(registration, "netFee")).isEqualByComparingTo(net);
    assertThat(amount(registration, "vat")).isEqualByComparingTo(gross.subtract(net));
    assertThat(amount(registration, "netFee").stripTrailingZeros().scale()).isLessThanOrEqualTo(2);
    assertThat(amount(registration, "vat").stripTrailingZeros().scale()).isLessThanOrEqualTo(2);
  }
}
