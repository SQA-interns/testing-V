package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static si.confreg.registration.testsupport.TestSettings.DEADLINE;
import static si.confreg.registration.testsupport.TestSettings.FEE_EARLY;
import static si.confreg.registration.testsupport.TestSettings.FEE_REGULAR;
import static si.confreg.registration.testsupport.TestSettings.ZONE;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import si.confreg.registration.testsupport.TestSettings;

class FeeCalculatorTest {

  private final FeeCalculator calculator = new FeeCalculator(TestSettings.settings());

  private static Instant local(int year, int month, int day, int hour, int minute, int second) {
    return java.time.LocalDateTime.of(year, month, day, hour, minute, second)
        .atZone(ZONE)
        .toInstant();
  }

  @Test
  void deadlineDayLastSecondInConferenceZoneIsEarly() {
    Fees fees = calculator.feesAt(local(2030, 1, 15, 23, 59, 59));
    assertThat(fees.netFee()).isEqualByComparingTo(FEE_EARLY);
  }

  @Test
  void dayAfterDeadlineFirstSecondInConferenceZoneIsRegular() {
    Fees fees = calculator.feesAt(DEADLINE.plusDays(1).atStartOfDay(ZONE).toInstant());
    assertThat(fees.netFee()).isEqualByComparingTo(FEE_REGULAR);
  }

  @Test
  void utcDateIsNotUsed() {
    // 2030-01-16 03:00 UTC is still 2030-01-15 in New York: early.
    Fees fees = calculator.feesAt(Instant.parse("2030-01-16T03:00:00Z"));
    assertThat(fees.netFee()).isEqualByComparingTo(FEE_EARLY);
  }

  @Test
  void vatIsRoundedHalfUpToCentsAndGrossIsTheSum() {
    // 150.50 x 0.255 = 38.3775 -> 38.38
    Fees fees = calculator.feesAt(local(2030, 2, 1, 12, 0, 0));
    assertThat(fees.vat()).isEqualByComparingTo("38.38");
    assertThat(fees.grossFee()).isEqualByComparingTo("188.88");
    assertThat(fees.netFee().scale()).isEqualTo(2);
    assertThat(fees.vat().scale()).isEqualTo(2);
    assertThat(fees.grossFee().scale()).isEqualTo(2);
  }

  @Test
  void exactHalfCentRoundsUp() {
    FeeCalculator small =
        new FeeCalculator(
            new si.confreg.registration.config.AppSettings(
                ZONE,
                DEADLINE,
                new BigDecimal("0.10"),
                FEE_REGULAR,
                new BigDecimal("0.25"),
                TestSettings.WORKSHOPS,
                1,
                si.confreg.registration.config.AppSettings.TestClockMode.DISABLED,
                "a@b.test",
                java.time.Duration.ofSeconds(1),
                1,
                new si.confreg.registration.config.AppSettings.Organizer("u", "p")));
    // 0.10 x 0.25 = 0.025 -> 0.03 (half-up)
    assertThat(small.feesAt(local(2030, 1, 1, 0, 0, 0)).vat()).isEqualByComparingTo("0.03");
  }

  @Test
  void configuredFeeWithoutDecimalsIsScaledToCents() {
    Fees fees = calculator.feesAt(local(2029, 12, 31, 8, 0, 0));
    assertThat(fees.netFee().toPlainString()).isEqualTo("100.00");
  }
}
