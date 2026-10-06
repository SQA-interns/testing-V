package si.confreg.registration.api;

import java.math.BigDecimal;
import si.confreg.registration.domain.Registration;

/** Stored registration as defined by the registration API contract. */
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
        registration.registrationNumber(),
        registration.firstName(),
        registration.lastName(),
        registration.email(),
        registration.payerType().value(),
        registration.companyName(),
        registration.companyAddress(),
        registration.companyVatId(),
        registration.workshop(),
        registration.netFee(),
        registration.vat(),
        registration.grossFee());
  }
}
