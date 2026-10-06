package si.confreg.registration.application;

import si.confreg.registration.domain.Registration;

/** Sends the confirmation e-mail of a stored registration (AC-001-04, AR-07). */
public interface ConfirmationSender {

  /**
   * Sends exactly one confirmation for the registration.
   *
   * @throws RuntimeException if the message cannot be handed to the SMTP server
   */
  void sendConfirmation(Registration registration);
}
