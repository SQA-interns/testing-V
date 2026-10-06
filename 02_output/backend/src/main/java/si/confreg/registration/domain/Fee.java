package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Conference fee of one registration: net fee, VAT and gross fee in EUR, two decimals. */
public record Fee(BigDecimal net, BigDecimal vat, BigDecimal gross) {

  public Fee {
    Objects.requireNonNull(net, "net");
    Objects.requireNonNull(vat, "vat");
    Objects.requireNonNull(gross, "gross");
    if (net.add(vat).compareTo(gross) != 0) {
      throw new IllegalArgumentException("gross fee must be net fee plus VAT");
    }
  }
}
