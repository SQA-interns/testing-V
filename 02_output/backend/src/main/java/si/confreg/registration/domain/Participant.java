package si.confreg.registration.domain;

/** A validated registration request; {@code company} is null for a private payer. */
public record Participant(
    String firstName,
    String lastName,
    String email,
    PayerType payerType,
    Company company,
    String workshopId) {}
