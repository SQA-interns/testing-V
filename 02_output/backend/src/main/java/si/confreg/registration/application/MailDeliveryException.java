package si.confreg.registration.application;

/** The SMTP server could not be reached or refused the message. */
public class MailDeliveryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MailDeliveryException(Throwable cause) {
    super("mail delivery failed", cause);
  }
}
