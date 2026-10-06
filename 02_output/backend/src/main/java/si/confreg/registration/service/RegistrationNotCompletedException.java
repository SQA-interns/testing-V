package si.confreg.registration.service;

/** The registration was rolled back because the confirmation e-mail was not accepted (D-22). */
public class RegistrationNotCompletedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RegistrationNotCompletedException(Throwable cause) {
    super("Registration not completed", cause);
  }
}
