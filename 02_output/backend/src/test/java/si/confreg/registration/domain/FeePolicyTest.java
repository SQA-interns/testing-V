package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the fee rule. The parameters are synthetic test inputs, deliberately different from
 * the configured business values (AR-04).
 */
class FeePolicyTest {

  private static final ZoneId ZONE = ZoneId.of("Pacific/Auckland");
  private static final LocalDate DEADLINE = LocalDate.of(2030, 3, 15);
  private static final BigDecimal EARLY = new BigDecimal("100.10");
  private static final BigDecimal REGULAR = new BigDecimal("150.15");
  private static final BigDecimal RATE = new BigDecimal("0.25");

  private final FeePolicy policy = new FeePolicy(ZONE, DEADLINE, EARLY, REGULAR, RATE);

  private static Instant local(int year, int month, int day, int hour, int minute, int second) {
    return LocalDate.of(year, month, day).atTime(hour, minute, second).atZone(ZONE).toInstant();
  }

  @Test
  void earlyFeeOnTheDeadlineDayUntilTheLastLocalSecond() {
    Fee fee = policy.feeAt(local(2030, 3, 15, 23, 59, 59));

    assertThat(fee.net()).isEqualByComparingTo(EARLY);
  }

  @Test
  void regularFeeFromTheFirstLocalSecondAfterTheDeadline() {
    Fee fee = policy.feeAt(local(2030, 3, 16, 0, 0, 0));

    assertThat(fee.net()).isEqualByComparingTo(REGULAR);
  }

  @Test
  void deadlineIsJudgedInTheConferenceZoneNotUtc() {
    // 2030-03-16 05:00 in Auckland is still 2030-03-15 in UTC, but locally after the deadline.
    assertThat(policy.feeAt(local(2030, 3, 16, 5, 0, 0)).net()).isEqualByComparingTo(REGULAR);
    assertThat(policy.feeAt(local(2030, 3, 15, 1, 0, 0)).net()).isEqualByComparingTo(EARLY);
  }

  @Test
  void vatIsRoundedHalfUpToCentsAndGrossIsNetPlusVat() {
    Fee fee = policy.feeAt(local(2030, 1, 1, 12, 0, 0));

    // 100.10 * 0.25 = 25.025 -> 25.03 (half up)
    assertThat(fee.vat()).isEqualByComparingTo("25.03");
    assertThat(fee.vat().scale()).isEqualTo(2);
    assertThat(fee.gross()).isEqualByComparingTo("125.13");
    assertThat(fee.net().scale()).isEqualTo(2);
  }

  @Test
  void amountsGivenWithoutDecimalsAreScaledToCents() {
    FeePolicy whole =
        new FeePolicy(ZONE, DEADLINE, new BigDecimal("80"), new BigDecimal("90"), RATE);

    Fee fee = whole.feeAt(local(2030, 1, 1, 12, 0, 0));

    assertThat(fee.net().toPlainString()).isEqualTo("80.00");
    assertThat(fee.vat().toPlainString()).isEqualTo("20.00");
    assertThat(fee.gross().toPlainString()).isEqualTo("100.00");
  }

  @Test
  void zeroVatRateIsAllowed() {
    FeePolicy noVat = new FeePolicy(ZONE, DEADLINE, EARLY, REGULAR, BigDecimal.ZERO);

    assertThat(noVat.feeAt(local(2030, 1, 1, 0, 0, 0)).vat()).isEqualByComparingTo("0");
  }

  @Test
  void rejectsInvalidConfiguration() {
    assertThatThrownBy(() -> new FeePolicy(ZONE, DEADLINE, BigDecimal.ZERO, REGULAR, RATE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FeePolicy(ZONE, DEADLINE, EARLY, new BigDecimal("-1"), RATE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FeePolicy(ZONE, DEADLINE, EARLY, REGULAR, new BigDecimal("-0.1")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FeePolicy(null, DEADLINE, EARLY, REGULAR, RATE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new FeePolicy(ZONE, null, EARLY, REGULAR, RATE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new FeePolicy(ZONE, DEADLINE, null, REGULAR, RATE))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new FeePolicy(ZONE, DEADLINE, EARLY, REGULAR, null))
        .isInstanceOf(NullPointerException.class);
  }
}
