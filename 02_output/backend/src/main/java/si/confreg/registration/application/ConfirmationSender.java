package si.confreg.registration.application;

import si.confreg.registration.domain.Registration;

/** Port to the confirmation e-mail (AC-001-03, AR-07). */
public interface ConfirmationSender {

  /**
   * Hands the confirmation to the SMTP server.
   *
   * @throws ConfirmationFailedException if the server does not accept it
   */
  void send(Registration registration);
}
