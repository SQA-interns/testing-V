package si.confreg.registration.mail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import si.confreg.registration.application.BusinessSettings;
import si.confreg.registration.domain.Company;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Participant;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.Workshop;

/** Renders docs/02_contracts/confirmation-email.yaml as plain text (no markup, SR-05). */
final class ConfirmationText {

  private ConfirmationText() {}

  static String subject(Registration registration) {
    return "Registration confirmation " + registration.registrationNumber();
  }

  static String body(Registration registration, BusinessSettings settings) {
    Participant p = registration.participant();
    Fees fees = registration.fees();
    String workshop =
        p.workshopId() == null
            ? "none"
            : settings.workshops().find(p.workshopId()).map(Workshop::title).orElse(p.workshopId());
    return "Dear "
        + p.firstName()
        + " "
        + p.lastName()
        + ",\n\n"
        + "thank you for registering for the conference. Your registration is confirmed.\n\n"
        + "Registration number: "
        + registration.registrationNumber()
        + "\nWorkshop: "
        + workshop
        + "\nFee (net): "
        + amount(fees.netFee())
        + " EUR\nVAT ("
        + percent(settings.vatRate())
        + " %): "
        + amount(fees.vat())
        + " EUR\nTotal (gross): "
        + amount(fees.grossFee())
        + " EUR\n\nPayer: "
        + payer(p.company())
        + "\n\nThe invoice will be sent to the payer by the conference accounting office.\n";
  }

  private static String amount(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String percent(BigDecimal rate) {
    return rate.movePointRight(2).stripTrailingZeros().toPlainString();
  }

  private static String payer(Company company) {
    if (company == null) {
      return "private";
    }
    return company.name() + ", " + company.address() + ", VAT ID " + company.vatId();
  }
}
