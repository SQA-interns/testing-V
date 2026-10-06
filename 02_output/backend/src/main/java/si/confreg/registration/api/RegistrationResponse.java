package si.confreg.registration.api;

import java.math.BigDecimal;
import si.confreg.registration.domain.Registration;

/** Stored registration as returned by the fixed registration API; amounts have two decimals. */
public record RegistrationResponse(
    String registrationNumber,
    String firstName,
    String lastName,
    String email,
    String payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    String workshop,
    BigDecimal netFee,
    BigDecimal vat,
    BigDecimal grossFee) {

  static RegistrationResponse of(Registration registration) {
    return new RegistrationResponse(
        registration.getRegistrationNumber(),
        registration.getFirstName(),
        registration.getLastName(),
        registration.getEmail(),
        registration.getPayerType().value(),
        registration.getCompanyName(),
        registration.getCompanyAddress(),
        registration.getCompanyVatId(),
        registration.getWorkshop(),
        registration.getNetFee(),
        registration.getVat(),
        registration.getGrossFee());
  }
}
