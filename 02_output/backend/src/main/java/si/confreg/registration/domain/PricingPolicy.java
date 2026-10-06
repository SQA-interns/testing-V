package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Early-bird or regular fee by submission instant (US-001 AC1, AC2; D-19). The configured fees are
 * gross; net is gross / (1 + VAT rate) rounded half-up to cents and VAT is the remainder.
 */
public final class PricingPolicy {

  private static final int CENTS = 2;

  private final Instant earlyBirdEnd;
  private final BigDecimal feeEarly;
  private final BigDecimal feeRegular;
  private final BigDecimal vatRate;

  public PricingPolicy(
      ZoneId conferenceZone,
      LocalDate earlyBirdDeadline,
      BigDecimal feeEarly,
      BigDecimal feeRegular,
      BigDecimal vatRate) {
    this.earlyBirdEnd = earlyBirdDeadline.plusDays(1).atStartOfDay(conferenceZone).toInstant();
    this.feeEarly = feeEarly;
    this.feeRegular = feeRegular;
    this.vatRate = vatRate;
  }

  /** Price of a registration submitted at {@code submittedAt}. */
  public Price priceAt(Instant submittedAt) {
    BigDecimal gross = isEarlyBird(submittedAt) ? feeEarly : feeRegular;
    return split(gross);
  }

  /** True up to and including the last instant of the deadline day in the conference zone. */
  public boolean isEarlyBird(Instant submittedAt) {
    return submittedAt.isBefore(earlyBirdEnd);
  }

  Price split(BigDecimal gross) {
    BigDecimal grossFee = gross.setScale(CENTS, RoundingMode.HALF_UP);
    BigDecimal netFee = grossFee.divide(BigDecimal.ONE.add(vatRate), CENTS, RoundingMode.HALF_UP);
    return new Price(netFee, grossFee.subtract(netFee), grossFee);
  }
}
