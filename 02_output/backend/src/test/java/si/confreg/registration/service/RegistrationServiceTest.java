package si.confreg.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import si.confreg.registration.config.AppProperties;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationInput;
import si.confreg.registration.mail.ConfirmationMailer;
import si.confreg.registration.mail.ConfirmationNotSentException;
import si.confreg.registration.persistence.RegistrationRepository;
import si.confreg.registration.time.AppClock;

class RegistrationServiceTest {

  private static final AppProperties PROPERTIES =
      new AppProperties(
          "Europe/Ljubljana",
          "2030-01-31",
          new BigDecimal("10.00"),
          new BigDecimal("20.00"),
          new BigDecimal("0.25"),
          "K1=Kappa",
          5,
          "disabled",
          "sender@example.com",
          false,
          false);

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final ConfirmationMailer mailer = mock(ConfirmationMailer.class);
  private final AppClock clock = mock(AppClock.class);
  private final RegistrationService service =
      new RegistrationService(repository, mailer, clock, PROPERTIES);

  private static RegistrationInput valid() {
    return new RegistrationInput(
        "Ana", "Kovač", "ana@example.com", "private", null, null, null, List.of("K1"));
  }

  @Test
  void registersPricesNumbersStoresAndConfirms() {
    Instant now = Instant.parse("2030-01-31T22:59:59Z");
    when(clock.now()).thenReturn(now);
    when(repository.nextRegistrationSequence()).thenReturn(42L);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Registration registration = service.register(valid());

    assertThat(registration.getRegistrationNumber()).isEqualTo("REG-000042");
    assertThat(registration.getGrossFee()).isEqualByComparingTo("10.00");
    assertThat(registration.getNetFee()).isEqualByComparingTo("8.00");
    assertThat(registration.getVat()).isEqualByComparingTo("2.00");
    assertThat(registration.getWorkshop()).isEqualTo("K1");
    assertThat(registration.getCreatedAt()).isEqualTo(now);
    verify(mailer).sendConfirmation(registration);
  }

  @Test
  void largeSequenceKeepsAllDigits() {
    when(clock.now()).thenReturn(Instant.parse("2031-01-01T00:00:00Z"));
    when(repository.nextRegistrationSequence()).thenReturn(1_234_567L);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Registration registration = service.register(valid());

    assertThat(registration.getRegistrationNumber()).isEqualTo("REG-1234567");
    assertThat(registration.getGrossFee()).isEqualByComparingTo("20.00");
  }

  @Test
  void invalidInputIsRejectedBeforeAnythingIsStoredOrSent() {
    RegistrationInput invalid =
        new RegistrationInput(null, "B", "bad", "private", "X", null, null, List.of("K9"));

    assertThatThrownBy(() -> service.register(invalid))
        .isInstanceOfSatisfying(
            InvalidRegistrationException.class,
            e ->
                assertThat(e.errors())
                    .extracting(si.confreg.registration.domain.FieldError::field)
                    .containsExactly("firstName", "email", "companyName", "workshops"));
    verifyNoInteractions(repository, mailer);
  }

  @Test
  void mailFailureBecomesNotCompleted() {
    when(clock.now()).thenReturn(Instant.parse("2030-01-01T00:00:00Z"));
    when(repository.nextRegistrationSequence()).thenReturn(1L);
    when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    doThrow(new ConfirmationNotSentException(new IllegalStateException("smtp")))
        .when(mailer)
        .sendConfirmation(any());

    assertThatThrownBy(() -> service.register(valid()))
        .isInstanceOf(RegistrationNotCompletedException.class)
        .hasCauseInstanceOf(ConfirmationNotSentException.class);
  }

  @Test
  void findDelegatesToRepository() {
    when(repository.findByRegistrationNumber("REG-000001")).thenReturn(Optional.empty());

    assertThat(service.find("REG-000001")).isEmpty();
    ArgumentCaptor<String> number = ArgumentCaptor.forClass(String.class);
    verify(repository).findByRegistrationNumber(number.capture());
    assertThat(number.getValue()).isEqualTo("REG-000001");
    verify(repository, never()).saveAndFlush(any());
  }
}
