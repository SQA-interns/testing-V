package si.confreg.registration.domain;

import java.math.BigDecimal;

/** Amounts in EUR with two decimals; {@code gross = net + vat}. */
public record Price(Tier tier, BigDecimal netFee, BigDecimal vat, BigDecimal grossFee) {

  /** Which rule produced the price (D-06, D-10). */
  public enum Tier {
    EARLY,
    REGULAR,
    STUDENT
  }
}
