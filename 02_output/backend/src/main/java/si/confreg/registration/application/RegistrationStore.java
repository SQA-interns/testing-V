package si.confreg.registration.application;

import java.util.Optional;
import si.confreg.registration.domain.Registration;

/** Port to registration storage (docs/02_contracts/registration-storage.sql). */
public interface RegistrationStore {

  /** Allocates the next registration number, e.g. {@code REG-000001}. */
  String nextRegistrationNumber();

  /** Stores the registration and flushes it, so storage errors surface before e-mail is sent. */
  void save(Registration registration);

  Optional<Registration> find(String registrationNumber);
}
