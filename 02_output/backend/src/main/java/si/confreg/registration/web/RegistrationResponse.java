package si.confreg.registration.web;

import java.math.BigDecimal;
import si.confreg.registration.domain.Payer;
import si.confreg.registration.domain.Registration;

/** Stored registration as JSON (spec 5.4; fixed fields of architecture.md plus status, time). */
record RegistrationResponse(
    String registrationNumber,
    String status,
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
    String submittedAt) {

  static RegistrationResponse of(Registration registration) {
    Payer payer = registration.payer();
    return new RegistrationResponse(
        registration.registrationNumber(),
        registration.status(),
        registration.firstName(),
        registration.lastName(),
        registration.email(),
        payer.type().code(),
        payer.companyName(),
        payer.companyAddress(),
        payer.companyVatId(),
        registration.workshopId(),
        registration.fee().net(),
        registration.fee().vat(),
        registration.fee().gross(),
        registration.submittedAt().toString());
  }
}
