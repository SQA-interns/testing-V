package si.confreg.registration.domain;

import java.util.Arrays;
import java.util.Optional;

/** Who pays the fee (fixed registration API: "private" or "company"). */
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

  public static Optional<PayerType> fromValue(String value) {
    return Arrays.stream(values()).filter(type -> type.value.equals(value)).findFirst();
  }
}
