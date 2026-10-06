package si.confreg.registration.application;

import java.io.IOException;

/** A request body is larger than the limit (SR-02); raised while the body is read. */
public class PayloadTooLargeException extends IOException {

  private static final long serialVersionUID = 1L;

  public PayloadTooLargeException() {
    super("request body too large");
  }
}
