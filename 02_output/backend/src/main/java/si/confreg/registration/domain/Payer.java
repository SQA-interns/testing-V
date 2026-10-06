package si.confreg.registration.domain;

import java.util.Objects;

/**
 * Payer of a registration. A private payer carries no company data (AC-001-05); a company payer
 * carries name, address and VAT ID.
 */
public record Payer(
    PayerType type, String companyName, String companyAddress, String companyVatId) {

  public Payer {
    Objects.requireNonNull(type, "type");
    if (type == PayerType.PRIVATE
        && (companyName != null || companyAddress != null || companyVatId != null)) {
      throw new IllegalArgumentException("a private payer has no company data");
    }
    if (type == PayerType.COMPANY
        && (companyName == null || companyAddress == null || companyVatId == null)) {
      throw new IllegalArgumentException("a company payer needs name, address and VAT ID");
    }
  }

  public static Payer privatePerson() {
    return new Payer(PayerType.PRIVATE, null, null, null);
  }

  public static Payer company(String name, String address, String vatId) {
    return new Payer(PayerType.COMPANY, name, address, vatId);
  }
}
