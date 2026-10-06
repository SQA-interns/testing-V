package si.confreg.registration.api;

import java.math.BigDecimal;
import java.time.Instant;
import si.confreg.registration.domain.Registration;

/** The stored registration as defined by the fixed API, plus {@code student} and time. */
record RegistrationResponse(
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
    BigDecimal grossFee,
    boolean student,
    Instant registeredAt) {

  static RegistrationResponse of(Registration registration) {
    return new RegistrationResponse(
        registration.getRegistrationNumber(),
        registration.getFirstName(),
        registration.getLastName(),
        registration.getEmail(),
        registration.getPayerType().apiValue(),
        registration.getCompanyName(),
        registration.getCompanyAddress(),
        registration.getCompanyVatId(),
        registration.getWorkshop(),
        registration.getNetFee().setScale(2),
        registration.getVat().setScale(2),
        registration.getGrossFee().setScale(2),
        registration.isStudent(),
        registration.getRegisteredAt());
  }
}
