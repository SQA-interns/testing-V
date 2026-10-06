package si.confreg.registration.application;

import java.util.List;

/** A registration request was rejected; nothing was stored. */
public class ValidationFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public ValidationFailedException(List<FieldError> errors) {
    super("registration rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
