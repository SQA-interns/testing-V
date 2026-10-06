package si.confreg.registration.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * One stored registration (REQ-REG-01 glossary). Immutable: a repeated registration is a new
 * instance with its own number and fee (AC-001-09).
 *
 * @param workshopId selected workshop id, or {@code null} for no workshop
 */
public record Registration(
    String registrationNumber,
    String firstName,
    String lastName,
    String email,
    Payer payer,
    String workshopId,
    Fee fee,
    Instant submittedAt) {

  /** The only status of a registration in this story. */
  public static final String STATUS_REGISTERED = "registered";

  public Registration {
    Objects.requireNonNull(registrationNumber, "registrationNumber");
    Objects.requireNonNull(firstName, "firstName");
    Objects.requireNonNull(lastName, "lastName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(payer, "payer");
    Objects.requireNonNull(fee, "fee");
    Objects.requireNonNull(submittedAt, "submittedAt");
  }

  public String status() {
    return STATUS_REGISTERED;
  }

  /** Copy with another registration number (used when a generated number collides). */
  public Registration withRegistrationNumber(String number) {
    return new Registration(
        number, firstName, lastName, email, payer, workshopId, fee, submittedAt);
  }
}
