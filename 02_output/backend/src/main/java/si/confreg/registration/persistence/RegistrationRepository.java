package si.confreg.registration.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import si.confreg.registration.domain.Registration;

/** Parameterised access to table {@code registration} (SB-05). */
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

  Optional<Registration> findByRegistrationNumber(String registrationNumber);

  /** Next value of {@code registration_number_seq}; unique even across concurrent requests. */
  @Query(value = "SELECT nextval('registration_number_seq')", nativeQuery = true)
  long nextRegistrationSequence();
}
