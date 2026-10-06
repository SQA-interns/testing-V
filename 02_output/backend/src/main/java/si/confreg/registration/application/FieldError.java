package si.confreg.registration.application;

import java.io.Serializable;

/** One rejected field of a request; {@code code} is one of the contract's error codes. */
public record FieldError(String field, String code, String message) implements Serializable {

  private static final long serialVersionUID = 1L;
}
