package si.confreg.registration.domain;

import java.util.Arrays;
import java.util.Optional;

/** Who pays the fee; the API values are fixed by the architecture. */
public enum PayerType {
  PRIVATE("private"),
  COMPANY("company");

  private final String apiValue;

  PayerType(String apiValue) {
    this.apiValue = apiValue;
  }

  public String apiValue() {
    return apiValue;
  }

  public static Optional<PayerType> fromApiValue(String value) {
    return Arrays.stream(values()).filter(p -> p.apiValue.equals(value)).findFirst();
  }
}
