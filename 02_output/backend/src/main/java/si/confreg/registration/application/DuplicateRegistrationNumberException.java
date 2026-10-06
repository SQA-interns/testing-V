package si.confreg.registration.application;

/** A generated registration number collided with a stored one. */
public class DuplicateRegistrationNumberException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateRegistrationNumberException(Throwable cause) {
    super("registration number already exists", cause);
  }
}
