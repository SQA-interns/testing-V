package si.confreg.registration.domain;

import java.util.List;

/** A registration request was rejected; carries exactly one error per rejected field. */
public class ValidationFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient List<FieldError> errors;

  public ValidationFailedException(List<FieldError> errors) {
    super("Registration request rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
