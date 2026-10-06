package si.confreg.registration.application;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.domain.Registration;

/** Reads a stored registration for an organizer (AC-001-05, AC-001-04). */
@Service
public class FindRegistration {

  private final RegistrationStore store;

  public FindRegistration(RegistrationStore store) {
    this.store = store;
  }

  @Transactional(readOnly = true)
  public Optional<Registration> find(String registrationNumber) {
    return store.find(registrationNumber);
  }
}
