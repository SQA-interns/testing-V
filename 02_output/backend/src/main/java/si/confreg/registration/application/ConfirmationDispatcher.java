package si.confreg.registration.application;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import si.confreg.registration.config.AppSettings;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.mail.ConfirmationMailer;
import si.confreg.registration.persistence.ConfirmationStatus;
import si.confreg.registration.persistence.RegistrationEntity;
import si.confreg.registration.persistence.RegistrationRepository;

/**
 * Sends exactly one confirmation per stored registration (AC-001-04): right after commit, and again
 * on a schedule while it is pending (D-12). Each send attempt is claimed atomically, so concurrent
 * senders never send the same confirmation twice. Logs carry the registration number only (SR-01).
 */
@Component
public class ConfirmationDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(ConfirmationDispatcher.class);

  private final RegistrationRepository repository;
  private final ConfirmationMailer mailer;
  private final ConferenceClock clock;
  private final AppSettings settings;
  private final TransactionTemplate newTransaction;

  public ConfirmationDispatcher(
      RegistrationRepository repository,
      ConfirmationMailer mailer,
      ConferenceClock clock,
      AppSettings settings,
      PlatformTransactionManager transactions) {
    this.repository = repository;
    this.mailer = mailer;
    this.clock = clock;
    this.settings = settings;
    this.newTransaction = new TransactionTemplate(transactions);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onStored(RegistrationStored event) {
    attempt(event.registrationId());
  }

  @Scheduled(
      initialDelayString = "${app.mail-retry-interval}",
      fixedDelayString = "${app.mail-retry-interval}")
  public void retryPending() {
    List<Long> pending =
        newTransaction.execute(
            status ->
                repository.findPendingConfirmations().stream()
                    .map(RegistrationEntity::id)
                    .toList());
    if (pending != null) {
      pending.forEach(this::attempt);
    }
  }

  void attempt(long registrationId) {
    Optional<Claim> claim =
        Optional.ofNullable(newTransaction.execute(status -> claim(registrationId)));
    if (claim.isEmpty()) {
      return;
    }
    Registration registration = claim.get().registration();
    try {
      mailer.send(registration);
      newTransaction.executeWithoutResult(
          status -> repository.markSent(registrationId, clock.now()));
    } catch (MailException e) {
      LOG.warn(
          "Confirmation for {} not sent (attempt {}): {}",
          registration.registrationNumber(),
          claim.get().attempt(),
          e.getClass().getName());
      if (claim.get().attempt() >= settings.mailMaxAttempts()) {
        newTransaction.executeWithoutResult(status -> repository.markFailed(registrationId));
        LOG.warn(
            "Confirmation for {} failed after {} attempts; manual follow-up needed",
            registration.registrationNumber(),
            claim.get().attempt());
      }
    }
  }

  private Claim claim(long registrationId) {
    return repository
        .findById(registrationId)
        .filter(entity -> ConfirmationStatus.PENDING.equals(entity.confirmationStatus()))
        .filter(
            entity -> repository.claimAttempt(registrationId, entity.confirmationAttempts()) == 1)
        .map(entity -> new Claim(entity.toRegistration(), entity.confirmationAttempts() + 1))
        .orElse(null);
  }

  private record Claim(Registration registration, int attempt) {}
}
