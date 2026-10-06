package si.confreg.registration.mail;

/** The confirmation e-mail could not be handed to the SMTP server (D-22). */
public class ConfirmationNotSentException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConfirmationNotSentException(Throwable cause) {
    super("Confirmation e-mail not sent", cause);
  }
}
