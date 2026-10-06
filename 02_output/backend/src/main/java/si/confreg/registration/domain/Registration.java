package si.confreg.registration.domain;

import java.math.BigDecimal;

/** A stored registration as exposed to the organizer and used for the confirmation. */
public record Registration(
    String registrationNumber,
    String firstName,
    String lastName,
    String email,
    PayerType payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    String workshop,
    BigDecimal netFee,
    BigDecimal vat,
    BigDecimal grossFee) {}
