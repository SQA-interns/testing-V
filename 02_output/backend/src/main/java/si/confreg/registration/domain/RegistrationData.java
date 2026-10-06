package si.confreg.registration.domain;

/**
 * A validated registration request: trimmed values; company fields only for a company payer;
 * workshop id or null.
 */
public record RegistrationData(
    String firstName,
    String lastName,
    String email,
    PayerType payerType,
    String companyName,
    String companyAddress,
    String companyVatId,
    String workshop) {}
