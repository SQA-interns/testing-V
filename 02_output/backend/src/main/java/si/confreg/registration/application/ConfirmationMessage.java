package si.confreg.registration.application;

import java.math.BigDecimal;
import java.util.Map;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

/**
 * Subject and plain-text body of the confirmation e-mail
 * (docs/02_contracts/confirmation-email.json; SR-05: values are inserted as plain text, nothing is
 * interpreted as markup or headers).
 */
public record ConfirmationMessage(String subject, String body) {

  public static ConfirmationMessage of(Registration registration, Map<String, String> workshops) {
    String number = registration.getRegistrationNumber();
    StringBuilder text = new StringBuilder(512);
    line(text, "Dear " + registration.getFirstName() + " " + registration.getLastName() + ",");
    line(text, "");
    line(text, "thank you for registering for the conference.");
    line(text, "");
    line(text, "Registration number: " + number);
    line(text, "Workshop: " + workshopLine(registration.getWorkshop(), workshops));
    line(text, "Payer: " + payerLine(registration));
    line(text, "");
    line(text, "Net fee: " + amount(registration.getNetFee()) + " EUR");
    line(text, "VAT: " + amount(registration.getVat()) + " EUR");
    line(text, "Total: " + amount(registration.getGrossFee()) + " EUR");
    line(text, "");
    line(text, paymentParagraph(registration));
    line(text, "");
    line(text, "Kind regards,");
    line(text, "The conference organizers");
    return new ConfirmationMessage("Registration confirmation " + number, text.toString());
  }

  private static void line(StringBuilder text, String line) {
    text.append(line).append("\r\n");
  }

  private static String workshopLine(String workshop, Map<String, String> workshops) {
    if (workshop == null) {
      return "none";
    }
    String title = workshops.get(workshop);
    return title == null ? workshop : title + " (" + workshop + ")";
  }

  private static String payerLine(Registration registration) {
    if (registration.getPayerType() == PayerType.PRIVATE) {
      return "private";
    }
    String line = registration.getCompanyName() + ", " + registration.getCompanyAddress();
    return registration.getCompanyVatId() == null
        ? line
        : line + ", VAT ID " + registration.getCompanyVatId();
  }

  private static String paymentParagraph(Registration registration) {
    if (registration.isStudent()) {
      return "As a student you participate free of charge. The organizer may ask you to show proof"
          + " of your student status.";
    }
    return "The invoice for "
        + amount(registration.getGrossFee())
        + " EUR will be sent to you separately by our accounting department. Please pay according"
        + " to the invoice.";
  }

  private static String amount(BigDecimal value) {
    return value.setScale(2).toPlainString();
  }
}
