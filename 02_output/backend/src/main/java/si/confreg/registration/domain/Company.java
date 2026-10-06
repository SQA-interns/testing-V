package si.confreg.registration.domain;

/** Company payer details used for the invoice by the accounting system (AC-001-04, D-20). */
public record Company(String name, String address, String vatId) {}
