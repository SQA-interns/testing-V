package si.confreg.registration.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationRepository;

/** Organizer read access to stored registrations (AC-001-28, AC-001-30). */
@Service
public class RegistrationQueryService {

  private final RegistrationRepository repository;

  public RegistrationQueryService(RegistrationRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Registration find(String registrationNumber) {
    return repository
        .findByRegistrationNumber(registrationNumber)
        .orElseThrow(RegistrationNotFoundException::new);
  }
}
