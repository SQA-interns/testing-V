package si.confreg.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingPolicyTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Ljubljana");
  private static final LocalDate DEADLINE = LocalDate.of(2030, 3, 10);
  private static final BigDecimal EARLY = new BigDecimal("100.00");
  private static final BigDecimal REGULAR = new BigDecimal("150.00");

  private final PricingPolicy policy =
      new PricingPolicy(ZONE, DEADLINE, EARLY, REGULAR, new BigDecimal("0.2"));

  @Test
  void earlyUntilLastMillisecondOfDeadlineDayInConferenceZone() {
    Instant end = DEADLINE.plusDays(1).atStartOfDay(ZONE).toInstant();

    assertThat(policy.isEarlyBird(end.minusMillis(1))).isTrue();
    assertThat(policy.isEarlyBird(end)).isFalse();
    assertThat(policy.priceAt(end.minusMillis(1)).grossFee()).isEqualByComparingTo(EARLY);
    assertThat(policy.priceAt(end).grossFee()).isEqualByComparingTo(REGULAR);
  }

  @Test
  void deadlineDayStartIsEarlyEvenThoughUtcDateIsPreviousDay() {
    Instant startOfDay = DEADLINE.atStartOfDay(ZONE).toInstant();

    assertThat(policy.isEarlyBird(startOfDay)).isTrue();
  }

  @Test
  void netAndVatSplitGrossWithTwoDecimals() {
    Price price = policy.priceAt(Instant.parse("2030-01-01T00:00:00Z"));

    assertThat(price.grossFee()).isEqualByComparingTo("100.00");
    assertThat(price.netFee()).isEqualByComparingTo("83.33");
    assertThat(price.vat()).isEqualByComparingTo("16.67");
    assertThat(price.netFee().add(price.vat())).isEqualByComparingTo(price.grossFee());
    assertThat(price.netFee().scale()).isEqualTo(2);
    assertThat(price.vat().scale()).isEqualTo(2);
  }

  @ParameterizedTest
  @CsvSource({
    "100.00, 0.22, 81.97, 18.03",
    "100.01, 0.095, 91.33, 8.68",
    "10.00, 0, 10.00, 0.00",
    "0.05, 0.5, 0.03, 0.02"
  })
  void splitRoundsNetHalfUp(String gross, String rate, String net, String vat) {
    PricingPolicy custom = new PricingPolicy(ZONE, DEADLINE, EARLY, REGULAR, new BigDecimal(rate));

    Price price = custom.split(new BigDecimal(gross));

    assertThat(price.netFee()).isEqualByComparingTo(net);
    assertThat(price.vat()).isEqualByComparingTo(vat);
    assertThat(price.grossFee()).isEqualByComparingTo(gross);
  }

  @Test
  void grossWithMoreDecimalsIsRoundedToCents() {
    Price price = policy.split(new BigDecimal("99.995"));

    assertThat(price.grossFee()).isEqualByComparingTo("100.00");
  }
}
