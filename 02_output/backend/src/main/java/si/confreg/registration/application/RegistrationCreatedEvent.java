package si.confreg.registration.application;

/** Published inside the registering transaction; mail is sent after commit (D-14). */
public record RegistrationCreatedEvent(long registrationId) {}
