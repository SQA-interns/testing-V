package si.confreg.registration.domain;

import java.util.Arrays;
import java.util.Optional;

/** Party the invoice will be addressed to (REQ-REG-01 glossary: payer). */
public enum PayerType {
  PRIVATE("private"),
  COMPANY("company");

  private final String code;

  PayerType(String code) {
    this.code = code;
  }

  /** The value used in the API and in storage. */
  public String code() {
    return code;
  }

  /** Exact, case-sensitive lookup by API code. */
  public static Optional<PayerType> fromCode(String code) {
    return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
  }
}
