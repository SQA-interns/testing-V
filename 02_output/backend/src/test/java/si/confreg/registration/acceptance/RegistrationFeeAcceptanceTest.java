package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** US-001: fee by early-bird deadline (AC-001-01, AC-001-02) and amounts (AC-001-06). */
class RegistrationFeeAcceptanceTest extends AcceptanceTestBase {

  private HttpResponse<String> registerAt(Instant now) {
    HttpResponse<String> response = postRegistration(privateRegistration(uniqueEmail("fee")), now);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    return response;
  }

  @Test
  void AC_001_01_registrationOnDeadlineDayStartHasEarlyFee() {
    assertThat(amount(registerAt(deadlineDayStart()), "netFee"))
        .isEqualByComparingTo(config("APP_FEE_EARLY"));
  }

  @Test
  void AC_001_01_registrationInLastSecondOfDeadlineDayInConferenceZoneHasEarlyFee() {
    assertThat(amount(registerAt(deadlineDayLastSecond()), "netFee"))
        .isEqualByComparingTo(config("APP_FEE_EARLY"));
  }

  @Test
  void AC_001_01_registrationWeeksBeforeDeadlineHasEarlyFee() {
    assertThat(amount(registerAt(deadlineDayStart().minusSeconds(30L * 24 * 3600)), "netFee"))
        .isEqualByComparingTo(config("APP_FEE_EARLY"));
  }

  @Test
  void AC_001_02_registrationAtStartOfDayAfterDeadlineInConferenceZoneHasRegularFee() {
    // still the deadline date in UTC, already the next day in the conference time zone
    assertThat(amount(registerAt(afterDeadline()), "netFee"))
        .isEqualByComparingTo(config("APP_FEE_REGULAR"));
  }

  @Test
  void AC_001_02_registrationWeeksAfterDeadlineHasRegularFee() {
    assertThat(amount(registerAt(afterDeadline().plusSeconds(30L * 24 * 3600)), "netFee"))
        .isEqualByComparingTo(config("APP_FEE_REGULAR"));
  }

  @Test
  void AC_001_06_earlyAmountsAreNetVatRoundedHalfUpAndGross() {
    HttpResponse<String> response = registerAt(deadlineDayStart());
    BigDecimal net = config("APP_FEE_EARLY");
    BigDecimal vat = expectedVat(net);

    assertThat(amount(response, "netFee")).isEqualByComparingTo(net);
    assertThat(amount(response, "vat")).isEqualByComparingTo(vat);
    assertThat(amount(response, "grossFee")).isEqualByComparingTo(net.add(vat));
  }

  @Test
  void AC_001_06_regularAmountsAreNetVatRoundedHalfUpAndGross() {
    HttpResponse<String> response = registerAt(afterDeadline());
    BigDecimal net = config("APP_FEE_REGULAR");
    BigDecimal vat = expectedVat(net);

    assertThat(amount(response, "netFee")).isEqualByComparingTo(net);
    assertThat(amount(response, "vat")).isEqualByComparingTo(vat);
    assertThat(amount(response, "grossFee")).isEqualByComparingTo(net.add(vat));
  }

  @Test
  void AC_001_06_amountsHaveTwoDecimals() {
    HttpResponse<String> response = registerAt(deadlineDayStart());
    for (String field : new String[] {"netFee", "vat", "grossFee"}) {
      assertThat(amount(response, field).stripTrailingZeros().scale())
          .as(field)
          .isLessThanOrEqualTo(2);
    }
  }
}
