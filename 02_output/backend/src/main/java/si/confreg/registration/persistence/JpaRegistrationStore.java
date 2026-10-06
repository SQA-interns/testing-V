package si.confreg.registration.persistence;

import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import si.confreg.registration.application.DuplicateRegistrationNumberException;
import si.confreg.registration.application.RegistrationStore;
import si.confreg.registration.domain.Registration;

/** {@link RegistrationStore} on PostgreSQL through JPA (AR-06, SB-05: bound parameters only). */
@Component
class JpaRegistrationStore implements RegistrationStore {

  static final String NUMBER_CONSTRAINT = "uq_registration_number";

  private final RegistrationJpaRepository repository;

  JpaRegistrationStore(RegistrationJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public Registration add(Registration registration) {
    try {
      return repository.saveAndFlush(RegistrationEntity.from(registration)).toDomain();
    } catch (DataIntegrityViolationException e) {
      if (isNumberCollision(e)) {
        throw new DuplicateRegistrationNumberException(e);
      }
      throw e;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Registration> findByNumber(String registrationNumber) {
    return repository
        .findByRegistrationNumber(registrationNumber)
        .map(RegistrationEntity::toDomain);
  }

  private static boolean isNumberCollision(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      String message = cause.getMessage();
      if (message != null && message.toLowerCase(Locale.ROOT).contains(NUMBER_CONSTRAINT)) {
        return true;
      }
    }
    return false;
  }
}
