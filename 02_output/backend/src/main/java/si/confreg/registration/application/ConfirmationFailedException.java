package si.confreg.registration.application;

/** The confirmation e-mail was not accepted; the registration is rolled back (D-25). */
public class ConfirmationFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConfirmationFailedException(Throwable cause) {
    super("Confirmation e-mail was not accepted", cause);
  }
}
