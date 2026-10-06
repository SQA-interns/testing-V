package si.confreg.registration.mail;

import java.math.BigDecimal;
import java.util.List;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.Workshop;
import si.confreg.registration.domain.WorkshopCatalog;

/**
 * Plain-text confirmation (contract: confirmation-email.json). User input appears only in the body;
 * the subject holds fixed text and the system-generated number (SR-05).
 */
final class ConfirmationMessage {

  static final String SUBJECT_PREFIX = "Registration confirmation ";
  static final String NO_WORKSHOP = "none";

  private final WorkshopCatalog workshops;

  ConfirmationMessage(WorkshopCatalog workshops) {
    this.workshops = workshops;
  }

  String subject(Registration registration) {
    return SUBJECT_PREFIX + registration.registrationNumber();
  }

  String body(Registration registration) {
    String workshop =
        registration.workshopId() == null
            ? NO_WORKSHOP
            : workshops
                .find(registration.workshopId())
                .map(Workshop::title)
                .orElse(registration.workshopId());
    List<String> lines =
        List.of(
            "Dear " + registration.firstName() + " " + registration.lastName() + ",",
            "",
            "your registration for the conference has been received.",
            "",
            "Registration number: " + registration.registrationNumber(),
            "Workshop: " + workshop,
            "Net fee: " + amount(registration.fee().net()),
            "VAT: " + amount(registration.fee().vat()),
            "Gross fee: " + amount(registration.fee().gross()),
            "",
            "Conference organizing committee");
    return String.join("\n", lines) + "\n";
  }

  private static String amount(BigDecimal value) {
    return value.toPlainString() + " EUR";
  }
}
