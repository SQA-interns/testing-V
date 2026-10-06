package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Fee by submission time (spec 4.1): early-bird fee if the submission date in the conference time
 * zone is on or before the deadline (inclusive), otherwise the regular fee; VAT at the configured
 * rate for every payer, rounded half up to 0.01.
 */
public final class FeePolicy {

  private static final int SCALE = 2;

  private final ZoneId conferenceZone;
  private final LocalDate earlyBirdDeadline;
  private final BigDecimal earlyFee;
  private final BigDecimal regularFee;
  private final BigDecimal vatRate;

  public FeePolicy(
      ZoneId conferenceZone,
      LocalDate earlyBirdDeadline,
      BigDecimal earlyFee,
      BigDecimal regularFee,
      BigDecimal vatRate) {
    this.conferenceZone = Objects.requireNonNull(conferenceZone, "conferenceZone");
    this.earlyBirdDeadline = Objects.requireNonNull(earlyBirdDeadline, "earlyBirdDeadline");
    this.earlyFee = requirePositive(earlyFee, "earlyFee");
    this.regularFee = requirePositive(regularFee, "regularFee");
    this.vatRate = Objects.requireNonNull(vatRate, "vatRate");
    if (vatRate.signum() < 0) {
      throw new IllegalArgumentException("vatRate must not be negative");
    }
  }

  private static BigDecimal requirePositive(BigDecimal value, String name) {
    Objects.requireNonNull(value, name);
    if (value.signum() <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }

  /** Fee for a registration submitted at {@code submittedAt}. */
  public Fee feeAt(Instant submittedAt) {
    LocalDate submissionDate = submittedAt.atZone(conferenceZone).toLocalDate();
    BigDecimal net =
        (submissionDate.isAfter(earlyBirdDeadline) ? regularFee : earlyFee)
            .setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal vat = net.multiply(vatRate).setScale(SCALE, RoundingMode.HALF_UP);
    return new Fee(net, vat, net.add(vat));
  }
}
