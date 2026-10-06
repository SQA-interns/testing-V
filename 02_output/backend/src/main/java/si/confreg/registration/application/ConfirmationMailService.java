package si.confreg.registration.application;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationRepository;

/**
 * Sends the confirmation e-mail after the registration is committed and retries unsent ones (D-14).
 * All sending runs on the single-threaded {@code mailScheduler}, so two sends never overlap. Logs
 * contain registration numbers only (SR-01).
 */
@Service
public class ConfirmationMailService {

  static final long RETRY_INTERVAL_MS = 30_000;
  private static final int BATCH = 50;
  private static final Logger LOG = LoggerFactory.getLogger(ConfirmationMailService.class);

  private final RegistrationRepository repository;
  private final MailGateway mailGateway;
  private final AppProperties properties;
  private final TimeSource timeSource;
  private final TaskExecutor mailScheduler;
  private final TransactionTemplate transactions;

  public ConfirmationMailService(
      RegistrationRepository repository,
      MailGateway mailGateway,
      AppProperties properties,
      TimeSource timeSource,
      @Qualifier("mailScheduler") TaskExecutor mailScheduler,
      PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.mailGateway = mailGateway;
    this.properties = properties;
    this.timeSource = timeSource;
    this.mailScheduler = mailScheduler;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRegistrationCreated(RegistrationCreatedEvent event) {
    mailScheduler.execute(() -> sendOne(event.registrationId()));
  }

  @Scheduled(
      fixedDelay = RETRY_INTERVAL_MS,
      initialDelay = RETRY_INTERVAL_MS,
      scheduler = "mailScheduler")
  public void retryUnsent() {
    List<Registration> pending =
        repository.findByConfirmationSentAtIsNullOrderByRegisteredAtAsc(PageRequest.of(0, BATCH));
    for (Registration registration : pending) {
      if (!sendOne(registration.getId())) {
        return; // SMTP still unavailable: wait for the next round
      }
    }
  }

  /** Returns {@code false} when sending failed. */
  boolean sendOne(long registrationId) {
    Registration registration = repository.findById(registrationId).orElse(null);
    if (registration == null || registration.getConfirmationSentAt() != null) {
      return true;
    }
    ConfirmationMessage message = ConfirmationMessage.of(registration, properties.workshops());
    try {
      mailGateway.send(registration.getEmail(), message.subject(), message.body());
    } catch (MailDeliveryException e) {
      LOG.warn(
          "Confirmation for {} not sent yet ({}); will retry",
          registration.getRegistrationNumber(),
          e.getCause() == null ? e.getClass().getSimpleName() : e.getCause().getClass().getName());
      update(registrationId, false);
      return false;
    }
    update(registrationId, true);
    LOG.info("Confirmation for {} sent", registration.getRegistrationNumber());
    return true;
  }

  private void update(long registrationId, boolean sent) {
    transactions.executeWithoutResult(
        status ->
            repository
                .findById(registrationId)
                .ifPresent(
                    r -> {
                      if (sent) {
                        r.markConfirmationSent(timeSource.now());
                      } else {
                        r.markConfirmationFailed();
                      }
                    }));
  }
}
