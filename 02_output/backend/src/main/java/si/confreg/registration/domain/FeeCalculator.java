package si.confreg.registration.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import si.confreg.registration.config.AppSettings;

/**
 * Fee for a submission time (AC-001-01 to 03, AC-001-06): early-bird fee when the submission date
 * in the conference time zone is on or before the deadline, otherwise the regular fee; VAT at the
 * configured rate for every payer, rounded half-up to 0.01.
 */
@Component
public class FeeCalculator {

  private static final int CENTS = 2;

  private final AppSettings settings;

  public FeeCalculator(AppSettings settings) {
    this.settings = settings;
  }

  public Fees feesAt(Instant submissionTime) {
    LocalDate submissionDate = submissionTime.atZone(settings.conferenceTz()).toLocalDate();
    BigDecimal net =
        submissionDate.isAfter(settings.earlyBirdDeadline())
            ? settings.feeRegular()
            : settings.feeEarly();
    net = net.setScale(CENTS, RoundingMode.HALF_UP);
    BigDecimal vat = net.multiply(settings.vatRate()).setScale(CENTS, RoundingMode.HALF_UP);
    return new Fees(net, vat, net.add(vat));
  }
}
