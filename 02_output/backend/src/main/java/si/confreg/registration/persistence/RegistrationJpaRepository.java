package si.confreg.registration.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RegistrationJpaRepository extends JpaRepository<RegistrationEntity, Long> {

  Optional<RegistrationEntity> findByRegistrationNumber(String registrationNumber);

  @Query(value = "SELECT nextval('registration_number_seq')", nativeQuery = true)
  long nextRegistrationSequence();
}
