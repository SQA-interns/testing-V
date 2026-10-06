package si.confreg.registration.domain;

import java.util.Optional;

/** Party the invoice will be addressed to. */
public enum PayerType {
  PRIVATE("private"),
  COMPANY("company");

  private final String value;

  PayerType(String value) {
    this.value = value;
  }

  /** Value used in the API and in storage. */
  public String value() {
    return value;
  }

  public static Optional<PayerType> fromValue(String value) {
    for (PayerType type : values()) {
      if (type.value.equals(value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
