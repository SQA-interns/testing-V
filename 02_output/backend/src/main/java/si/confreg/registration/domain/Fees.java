package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Net fee, VAT and gross fee in EUR with two decimals (AC-001-06, D-21). */
public record Fees(BigDecimal netFee, BigDecimal vat, BigDecimal grossFee) {

  private static final int SCALE = 2;

  /** VAT is the net fee times the rate, rounded half-up to cents; gross is net plus VAT. */
  public static Fees fromNet(BigDecimal netFee, BigDecimal vatRate) {
    BigDecimal net = netFee.setScale(SCALE, RoundingMode.HALF_UP);
    BigDecimal vat = net.multiply(vatRate).setScale(SCALE, RoundingMode.HALF_UP);
    return new Fees(net, vat, net.add(vat));
  }
}
