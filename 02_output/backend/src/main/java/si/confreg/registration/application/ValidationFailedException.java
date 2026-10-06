package si.confreg.registration.application;

import java.util.ArrayList;
import java.util.List;

/** A registration request was rejected; nothing was stored. */
public class ValidationFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ArrayList<FieldError> errors;

  public ValidationFailedException(List<FieldError> errors) {
    super("registration rejected");
    this.errors = new ArrayList<>(errors);
  }

  public List<FieldError> errors() {
    return List.copyOf(errors);
  }
}
