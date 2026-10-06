package si.confreg.registration.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link RegistrationEntity}. */
interface RegistrationJpaRepository extends JpaRepository<RegistrationEntity, Long> {

  Optional<RegistrationEntity> findByRegistrationNumber(String registrationNumber);
}
