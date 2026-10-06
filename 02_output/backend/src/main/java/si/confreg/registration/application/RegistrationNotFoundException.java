package si.confreg.registration.application;

/** No registration has the requested number. */
public class RegistrationNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RegistrationNotFoundException() {
    super("registration not found");
  }
}
