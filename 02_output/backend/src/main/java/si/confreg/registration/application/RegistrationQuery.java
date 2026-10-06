package si.confreg.registration.application;

import java.util.Optional;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationNumbers;

/** Use case: the organizer reads one stored registration (AC-001-10). */
public final class RegistrationQuery {

  private final RegistrationStore store;

  public RegistrationQuery(RegistrationStore store) {
    this.store = store;
  }

  public Optional<Registration> find(String registrationNumber) {
    if (!RegistrationNumbers.isWellFormed(registrationNumber)) {
      return Optional.empty();
    }
    return store.findByNumber(registrationNumber);
  }
}
