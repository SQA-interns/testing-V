package si.confreg.registration.api;

/** The request body is not a JSON object. */
class MalformedRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  MalformedRequestException() {
    super("Malformed request body");
  }
}
