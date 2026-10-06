package si.confreg.registration.service;

import java.util.List;
import si.confreg.registration.domain.FieldError;

/** The submitted registration violates one or more rules; nothing was stored. */
public class InvalidRegistrationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public InvalidRegistrationException(List<FieldError> errors) {
    super("Invalid registration");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
