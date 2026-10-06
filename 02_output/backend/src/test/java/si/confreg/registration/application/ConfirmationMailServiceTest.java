package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationRepository;

class ConfirmationMailServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final MailGateway gateway = mock(MailGateway.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
  private ConfirmationMailService service;

  @BeforeEach
  void setUp() {
    TransactionStatus status = new SimpleTransactionStatus();
    when(transactions.getTransaction(any())).thenReturn(status);
    service =
        new ConfirmationMailService(
            repository,
            gateway,
            TestSettings.properties(),
            () -> NOW,
            new SyncTaskExecutor(),
            transactions);
  }

  private Registration stored(long id) {
    Registration registration =
        ConfirmationMessageTest.registration(PayerType.PRIVATE, null, null, false, "1.00");
    when(repository.findById(id)).thenReturn(Optional.of(registration));
    return registration;
  }

  @Test
  void sendsAfterCommitAndMarksSent() {
    Registration registration = stored(1L);

    service.onRegistrationCreated(new RegistrationCreatedEvent(1L));

    verify(gateway)
        .send(eq("ziga@example.org"), eq("Registration confirmation CR-000007"), anyString());
    assertThat(registration.getConfirmationSentAt()).isEqualTo(NOW);
    assertThat(registration.getConfirmationAttempts()).isEqualTo(1);
  }

  @Test
  void failedSendIsCountedAndNotMarkedSent() {
    Registration registration = stored(1L);
    doThrow(new MailDeliveryException(new RuntimeException("down")))
        .when(gateway)
        .send(anyString(), anyString(), anyString());

    assertThat(service.sendOne(1L)).isFalse();

    assertThat(registration.getConfirmationSentAt()).isNull();
    assertThat(registration.getConfirmationAttempts()).isEqualTo(1);
  }

  @Test
  void alreadySentOrMissingRegistrationIsSkipped() {
    Registration registration = stored(1L);
    registration.markConfirmationSent(NOW);
    when(repository.findById(2L)).thenReturn(Optional.empty());

    assertThat(service.sendOne(1L)).isTrue();
    assertThat(service.sendOne(2L)).isTrue();

    verify(gateway, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void retryStopsAtFirstFailure() {
    Registration first = stored(1L);
    Registration second =
        ConfirmationMessageTest.registration(PayerType.PRIVATE, null, null, false, "1.00");
    setId(first, 1L);
    setId(second, 2L);
    when(repository.findById(2L)).thenReturn(Optional.of(second));
    when(repository.findByConfirmationSentAtIsNullOrderByRegisteredAtAsc(any(Pageable.class)))
        .thenReturn(List.of(first, second));
    doThrow(new MailDeliveryException(null))
        .when(gateway)
        .send(anyString(), anyString(), anyString());

    service.retryUnsent();

    verify(gateway, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  void retrySendsAllPending() {
    Registration first = stored(1L);
    setId(first, 1L);
    when(repository.findByConfirmationSentAtIsNullOrderByRegisteredAtAsc(any(Pageable.class)))
        .thenReturn(List.of(first));

    service.retryUnsent();

    assertThat(first.getConfirmationSentAt()).isEqualTo(NOW);
  }

  private static void setId(Registration registration, long id) {
    try {
      var field = Registration.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(registration, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
