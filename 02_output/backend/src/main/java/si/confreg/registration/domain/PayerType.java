package si.confreg.registration.domain;

import java.util.Optional;

/** Who pays the fee; the API value is lower case. */
public enum PayerType {
  PRIVATE("private"),
  COMPANY("company");

  private final String value;

  PayerType(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  /** Exact, case-sensitive match on the API value. */
  public static Optional<PayerType> fromValue(String value) {
    for (PayerType type : values()) {
      if (type.value.equals(value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
