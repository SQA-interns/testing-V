package si.confreg.registration.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Persistence of registrations (AR-06: the schema itself is owned by Flyway). */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

  boolean existsByEmailNormalized(String emailNormalized);

  Optional<Registration> findByRegistrationNumber(String registrationNumber);

  List<Registration> findAllByOrderByRegistrationNumberAsc();

  List<Registration> findByConfirmationSentAtIsNullOrderByRegisteredAtAsc(Pageable pageable);

  @Query(value = "SELECT nextval('registration_number_seq')", nativeQuery = true)
  long nextRegistrationSequence();
}
