package si.confreg.registration.application;

/** One rejected field of a request; {@code code} is one of the contract's error codes. */
public record FieldError(String field, String code, String message) {}
