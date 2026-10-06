package si.confreg.registration.application;

/** Published inside the registering transaction; handled after commit. */
public record RegistrationStored(long registrationId) {}
