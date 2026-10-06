package si.confreg.registration.api;

import java.math.BigDecimal;
import si.confreg.registration.domain.Company;
import si.confreg.registration.domain.Participant;
import si.confreg.registration.domain.Registration;

/** Stored registration as fixed by architecture.md (amounts as JSON numbers, two decimals). */
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
    BigDecimal grossFee) {

  static RegistrationResponse from(Registration registration) {
    Participant p = registration.participant();
    Company c = p.company();
    return new RegistrationResponse(
        registration.registrationNumber(),
        p.firstName(),
        p.lastName(),
        p.email(),
        p.payerType().value(),
        c == null ? null : c.name(),
        c == null ? null : c.address(),
        c == null ? null : c.vatId(),
        p.workshopId(),
        registration.fees().netFee(),
        registration.fees().vat(),
        registration.fees().grossFee());
  }
}
