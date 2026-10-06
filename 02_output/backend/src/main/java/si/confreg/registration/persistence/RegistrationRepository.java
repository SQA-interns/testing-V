package si.confreg.registration.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Access to stored registrations; bound parameters only (SB-05). */
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, Long> {

  Optional<RegistrationEntity> findByRegistrationNumber(String registrationNumber);

  boolean existsByRegistrationNumber(String registrationNumber);

  @Query(
      "select r from RegistrationEntity r where r.confirmationStatus = '"
          + ConfirmationStatus.PENDING
          + "' order by r.id")
  List<RegistrationEntity> findPendingConfirmations();

  /**
   * Claims the next send attempt; only one caller wins for a given attempt count.
   *
   * @return 1 if claimed, 0 if another caller claimed it or it is no longer pending
   */
  @Modifying
  @Query(
      "update RegistrationEntity r set r.confirmationAttempts = r.confirmationAttempts + 1"
          + " where r.id = :id and r.confirmationAttempts = :attempts"
          + " and r.confirmationStatus = '"
          + ConfirmationStatus.PENDING
          + "'")
  int claimAttempt(@Param("id") long id, @Param("attempts") int attempts);

  @Modifying
  @Query(
      "update RegistrationEntity r set r.confirmationStatus = '"
          + ConfirmationStatus.SENT
          + "', r.confirmationSentAt = :sentAt where r.id = :id")
  int markSent(@Param("id") long id, @Param("sentAt") Instant sentAt);

  @Modifying
  @Query(
      "update RegistrationEntity r set r.confirmationStatus = '"
          + ConfirmationStatus.FAILED
          + "' where r.id = :id and r.confirmationStatus = '"
          + ConfirmationStatus.PENDING
          + "'")
  int markFailed(@Param("id") long id);
}
