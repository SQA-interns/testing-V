package si.confreg.registration.api;

/** No registration with the requested number. */
class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  NotFoundException() {
    super("Not found");
  }
}
