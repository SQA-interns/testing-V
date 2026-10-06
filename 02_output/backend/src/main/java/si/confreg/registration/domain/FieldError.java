package si.confreg.registration.domain;

import java.io.Serializable;

/** One rejected request field and the reason, safe to show to the participant. */
public record FieldError(String field, String message) implements Serializable {}
