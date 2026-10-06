package si.confreg.registration.domain;

/** One invalid input field; the message never repeats the submitted value (SR-01). */
public record FieldError(String field, String message) {}
