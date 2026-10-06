package si.confreg.registration.application;

/** A registration with the same normalised e-mail address exists (D-12). */
public class DuplicateRegistrationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateRegistrationException() {
    super("duplicate registration");
  }
}
