package si.confreg.registration.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import si.confreg.registration.domain.Price;

/** Fee calculation of US-001 [2], [3] (D-06, D-09, D-10). */
@Service
public class PricingService {

  private static final int CENTS = 2;

  private final AppProperties properties;

  public PricingService(AppProperties properties) {
    this.properties = properties;
  }

  public Price price(boolean student, Instant registrationTime) {
    if (student) {
      BigDecimal zero = BigDecimal.ZERO.setScale(CENTS);
      return new Price(Price.Tier.STUDENT, zero, zero, zero);
    }
    LocalDate localDate = registrationTime.atZone(properties.conferenceTz()).toLocalDate();
    boolean early = !localDate.isAfter(properties.earlyBirdDeadline());
    BigDecimal net =
        (early ? properties.feeEarly() : properties.feeRegular())
            .setScale(CENTS, RoundingMode.HALF_UP);
    BigDecimal vat = net.multiply(properties.vatRate()).setScale(CENTS, RoundingMode.HALF_UP);
    return new Price(early ? Price.Tier.EARLY : Price.Tier.REGULAR, net, vat, net.add(vat));
  }
}
