package si.confreg.registration.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.mail.ConfirmationMailer;
import si.confreg.registration.persistence.ConfirmationStatus;
import si.confreg.registration.persistence.RegistrationEntity;
import si.confreg.registration.persistence.RegistrationRepository;
import si.confreg.registration.testsupport.TestSettings;

/** One confirmation per registration with claimed, retried attempts (AC-001-04, D-12). */
class ConfirmationDispatcherTest {

  private static final long ID = 7L;
  private static final Instant NOW = Instant.parse("2030-01-10T10:00:00Z");

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final ConfirmationMailer mailer = mock(ConfirmationMailer.class);
  private final ConferenceClock clock = mock(ConferenceClock.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
  private final RegistrationEntity entity = mock(RegistrationEntity.class);
  private final Registration registration =
      new Registration(
          "CR-0000000001",
          "Ana",
          "Novak",
          "ana@example.org",
          PayerType.PRIVATE,
          null,
          null,
          null,
          null,
          BigDecimal.ONE,
          BigDecimal.ZERO,
          BigDecimal.ONE);
  private ConfirmationDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    when(clock.now()).thenReturn(NOW);
    when(entity.id()).thenReturn(ID);
    when(entity.toRegistration()).thenReturn(registration);
    when(entity.confirmationStatus()).thenReturn(ConfirmationStatus.PENDING);
    when(entity.confirmationAttempts()).thenReturn(0);
    when(repository.findById(ID)).thenReturn(Optional.of(entity));
    when(repository.claimAttempt(ID, 0)).thenReturn(1);
    dispatcher =
        new ConfirmationDispatcher(
            repository, mailer, clock, TestSettings.settings(), transactions);
  }

  @Test
  void storedRegistrationIsSentOnceAndMarkedSent() {
    dispatcher.onStored(new RegistrationStored(ID));

    verify(mailer).send(registration);
    verify(repository).markSent(ID, NOW);
  }

  @Test
  void eachStepRunsInItsOwnNewTransaction() {
    dispatcher.onStored(new RegistrationStored(ID));

    org.mockito.ArgumentCaptor<org.springframework.transaction.TransactionDefinition> definitions =
        org.mockito.ArgumentCaptor.forClass(
            org.springframework.transaction.TransactionDefinition.class);
    verify(transactions, org.mockito.Mockito.atLeastOnce()).getTransaction(definitions.capture());
    org.assertj.core.api.Assertions.assertThat(definitions.getAllValues())
        .allSatisfy(
            definition ->
                org.assertj.core.api.Assertions.assertThat(definition.getPropagationBehavior())
                    .isEqualTo(
                        org.springframework.transaction.TransactionDefinition
                            .PROPAGATION_REQUIRES_NEW));
  }

  @Test
  void lostClaimSendsNothing() {
    when(repository.claimAttempt(ID, 0)).thenReturn(0);

    dispatcher.onStored(new RegistrationStored(ID));

    verify(mailer, never()).send(any());
    verify(repository, never()).markSent(anyLong(), any());
  }

  @Test
  void alreadySentRegistrationIsNotClaimed() {
    when(entity.confirmationStatus()).thenReturn(ConfirmationStatus.SENT);

    dispatcher.onStored(new RegistrationStored(ID));

    verify(repository, never()).claimAttempt(anyLong(), anyInt());
    verify(mailer, never()).send(any());
  }

  @Test
  void unknownRegistrationIsIgnored() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    dispatcher.onStored(new RegistrationStored(ID));

    verify(mailer, never()).send(any());
  }

  @Test
  void failedSendStaysPendingBeforeTheLastAttempt() {
    doThrow(new MailSendException("down")).when(mailer).send(registration);

    dispatcher.onStored(new RegistrationStored(ID));

    verify(repository, never()).markSent(anyLong(), any());
    verify(repository, never()).markFailed(anyLong());
  }

  @Test
  void failedSendOnTheLastAttemptIsMarkedFailed() {
    int beforeLast = TestSettings.MAX_ATTEMPTS - 1;
    when(entity.confirmationAttempts()).thenReturn(beforeLast);
    when(repository.claimAttempt(ID, beforeLast)).thenReturn(1);
    doThrow(new MailSendException("down")).when(mailer).send(registration);

    dispatcher.onStored(new RegistrationStored(ID));

    verify(repository).markFailed(ID);
  }

  @Test
  void retryAttemptsEveryPendingRegistration() {
    when(repository.findPendingConfirmations()).thenReturn(List.of(entity));

    dispatcher.retryPending();

    verify(mailer).send(registration);
    verify(repository).markSent(eq(ID), any());
  }

  @Test
  void retryWithNothingPendingSendsNothing() {
    when(repository.findPendingConfirmations()).thenReturn(List.of());

    dispatcher.retryPending();

    verify(mailer, never()).send(any());
  }
}
