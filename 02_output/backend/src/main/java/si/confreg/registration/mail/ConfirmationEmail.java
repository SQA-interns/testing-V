package si.confreg.registration.mail;

import java.math.BigDecimal;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

/**
 * Subject and plain-text body of the confirmation e-mail
 * (docs/02_contracts/confirmation-email.yaml). User input appears only in the body, never in a
 * header (SR-05).
 */
record ConfirmationEmail(String subject, String body) {

  static ConfirmationEmail of(Registration registration, String workshopName, BigDecimal vatRate) {
    String subject = "Registration confirmation " + registration.getRegistrationNumber();
    String workshopLine =
        registration.getWorkshop() == null
            ? "none"
            : registration.getWorkshop() + " - " + workshopName;
    String payerLine =
        registration.getPayerType() == PayerType.COMPANY
            ? registration.getCompanyName()
                + ", "
                + registration.getCompanyAddress()
                + ", VAT ID "
                + registration.getCompanyVatId()
            : registration.getFirstName() + " " + registration.getLastName();
    String vatPercent = vatRate.movePointRight(2).stripTrailingZeros().toPlainString();
    String body =
        "Dear "
            + registration.getFirstName()
            + " "
            + registration.getLastName()
            + ",\n\n"
            + "thank you for registering for the conference.\n\n"
            + "Registration number: "
            + registration.getRegistrationNumber()
            + "\n"
            + "Workshop: "
            + workshopLine
            + "\n"
            + "Fee: "
            + amount(registration.getGrossFee())
            + " EUR (net "
            + amount(registration.getNetFee())
            + " EUR + VAT "
            + vatPercent
            + "% "
            + amount(registration.getVat())
            + " EUR)\n\n"
            + "Payer: "
            + payerLine
            + "\n\n"
            + "The invoice will be sent to the payer by the organizer's accounting department.\n\n"
            + "Kind regards,\n"
            + "The conference organizers\n";
    return new ConfirmationEmail(subject, body);
  }

  private static String amount(BigDecimal value) {
    return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }
}
