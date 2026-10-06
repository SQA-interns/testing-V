package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import si.confreg.registration.domain.Price;

class PricingServiceTest {

  private final PricingService pricing = new PricingService(TestSettings.properties());

  @Test
  void earlyBirdBeforeDeadline() {
    Price price = pricing.price(false, Instant.parse("2026-07-01T10:00:00Z"));

    assertThat(price.tier()).isEqualTo(Price.Tier.EARLY);
    assertThat(price.netFee()).isEqualByComparingTo("240.00");
    assertThat(price.vat()).isEqualByComparingTo("52.80");
    assertThat(price.grossFee()).isEqualByComparingTo("292.80");
  }

  @Test
  void deadlineDayIsInclusiveInConferenceTimeZone() {
    // 2026-07-31T23:59:59 in Ljubljana (UTC+2)
    assertThat(pricing.price(false, Instant.parse("2026-07-31T21:59:59Z")).tier())
        .isEqualTo(Price.Tier.EARLY);
    // 2026-08-01T00:00:00 in Ljubljana, still 31 July in UTC
    assertThat(pricing.price(false, Instant.parse("2026-07-31T22:00:00Z")).tier())
        .isEqualTo(Price.Tier.REGULAR);
  }

  @Test
  void regularAfterDeadline() {
    Price price = pricing.price(false, Instant.parse("2026-09-01T10:00:00Z"));

    assertThat(price.tier()).isEqualTo(Price.Tier.REGULAR);
    assertThat(price.netFee()).isEqualByComparingTo("300.00");
    assertThat(price.vat()).isEqualByComparingTo("66.00");
    assertThat(price.grossFee()).isEqualByComparingTo("366.00");
  }

  @Test
  void studentIsFreeWithTwoDecimals() {
    Price price = pricing.price(true, Instant.parse("2026-09-01T10:00:00Z"));

    assertThat(price.tier()).isEqualTo(Price.Tier.STUDENT);
    assertThat(price.netFee().toPlainString()).isEqualTo("0.00");
    assertThat(price.vat().toPlainString()).isEqualTo("0.00");
    assertThat(price.grossFee().toPlainString()).isEqualTo("0.00");
  }

  @Test
  void vatIsRoundedHalfUpAndAmountsHaveScaleTwo() {
    PricingService custom = new PricingService(TestSettings.with("feeRegular", "222.26"));
    // 222.26 * 0.22 = 48.8972 -> 48.90
    Price price = custom.price(false, Instant.parse("2026-09-01T10:00:00Z"));

    assertThat(price.vat().toPlainString()).isEqualTo("48.90");
    assertThat(price.grossFee().toPlainString()).isEqualTo("271.16");

    PricingService half =
        new PricingService(
            TestSettings.properties(withValues("feeRegular", "222.26", "vatRate", "0.25")));
    // 222.26 * 0.25 = 55.565 -> half-up 55.57 (half-even would be 55.56)
    assertThat(half.price(false, Instant.parse("2026-09-01T10:00:00Z")).vat().toPlainString())
        .isEqualTo("55.57");
  }

  @Test
  void feeWithoutDecimalsIsShownWithTwo() {
    PricingService custom = new PricingService(TestSettings.with("feeEarly", "240"));

    assertThat(custom.price(false, Instant.parse("2026-07-01T10:00:00Z")).netFee().toPlainString())
        .isEqualTo("240.00");
  }

  private static java.util.Map<String, String> withValues(String... pairs) {
    java.util.Map<String, String> values = TestSettings.defaults();
    for (int i = 0; i < pairs.length; i += 2) {
      values.put(pairs[i], pairs[i + 1]);
    }
    return values;
  }
}
