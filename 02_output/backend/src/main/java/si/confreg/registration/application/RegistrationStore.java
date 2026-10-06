package si.confreg.registration.application;

import java.util.Optional;
import si.confreg.registration.domain.Registration;

/** Storage port for registrations. Registrations are only added, never changed (AC-001-09). */
public interface RegistrationStore {

  /**
   * Stores a new registration and commits it.
   *
   * @throws DuplicateRegistrationNumberException if the number is already taken
   */
  Registration add(Registration registration);

  Optional<Registration> findByNumber(String registrationNumber);
}
