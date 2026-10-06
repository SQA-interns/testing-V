package si.confreg.registration.api;

/** No registration has the requested number. */
class RegistrationNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  RegistrationNotFoundException() {
    super("Registration not found");
  }
}
