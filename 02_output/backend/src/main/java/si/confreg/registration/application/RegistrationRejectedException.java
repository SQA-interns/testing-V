package si.confreg.registration.application;

import java.util.List;

/** The registration request has invalid fields (AC-001-08). */
public class RegistrationRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final List<String> invalidFields;

  public RegistrationRejectedException(List<String> invalidFields) {
    super("Registration rejected");
    this.invalidFields = List.copyOf(invalidFields);
  }

  public List<String> invalidFields() {
    return invalidFields;
  }
}
