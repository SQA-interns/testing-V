package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationRepository;

class RegistrationServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-01T10:00:00.123456789Z");

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final AppProperties properties = TestSettings.properties();
  private final RegistrationService service =
      new RegistrationService(
          new RegistrationValidator(properties),
          new PricingService(properties),
          repository,
          () -> NOW,
          events);

  /** Assigns the id as JPA does on insert. */
  private static Registration withId(Registration registration) {
    try {
      var field = Registration.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(registration, 7L);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    return registration;
  }

  private static RegistrationCommand command() {
    return new RegistrationCommand(
        "Ana",
        "Novak",
        " Ana@Example.org ",
        "private",
        null,
        null,
        null,
        List.of("W1"),
        false,
        Set.of());
  }

  @Test
  void storesPricedRegistrationWithFormattedNumberAndPublishesEvent() {
    when(repository.nextRegistrationSequence()).thenReturn(42L);
    when(repository.saveAndFlush(any(Registration.class)))
        .thenAnswer(i -> withId(i.getArgument(0)));

    Registration saved = service.register(command());

    assertThat(saved.getRegistrationNumber()).isEqualTo("CR-000042");
    assertThat(saved.getEmail()).isEqualTo("Ana@Example.org");
    assertThat(saved.getWorkshop()).isEqualTo("W1");
    assertThat(saved.getNetFee()).isEqualByComparingTo("240.00");
    assertThat(saved.getRegisteredAt()).isEqualTo(Instant.parse("2026-07-01T10:00:00.123456Z"));
    verify(repository).existsByEmailNormalized("ana@example.org");
    verify(events).publishEvent(any(RegistrationCreatedEvent.class));
  }

  @Test
  void existingEmailIsDuplicate() {
    when(repository.existsByEmailNormalized("ana@example.org")).thenReturn(true);

    assertThatThrownBy(() -> service.register(command()))
        .isInstanceOf(DuplicateRegistrationException.class);
    verify(repository, never()).saveAndFlush(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void raceOnUniqueEmailIndexIsDuplicate() {
    when(repository.saveAndFlush(any()))
        .thenThrow(
            new DataIntegrityViolationException(
                "x",
                new SQLException(
                    "duplicate key value violates unique constraint \"uk_registration_email\"")));

    assertThatThrownBy(() -> service.register(command()))
        .isInstanceOf(DuplicateRegistrationException.class);
  }

  @Test
  void otherIntegrityViolationIsRethrown() {
    DataIntegrityViolationException other =
        new DataIntegrityViolationException("x", new SQLException("ck_registration_amounts"));
    when(repository.saveAndFlush(any())).thenThrow(other);

    assertThatThrownBy(() -> service.register(command())).isSameAs(other);
  }

  @Test
  void invalidCommandStoresNothing() {
    RegistrationCommand invalid =
        new RegistrationCommand(null, null, null, null, null, null, null, null, false, Set.of());

    assertThatThrownBy(() -> service.register(invalid))
        .isInstanceOf(ValidationFailedException.class);
    verify(repository, never()).nextRegistrationSequence();
  }

  @Test
  void studentIsStoredFree() {
    when(repository.saveAndFlush(any(Registration.class)))
        .thenAnswer(i -> withId(i.getArgument(0)));
    RegistrationCommand student =
        new RegistrationCommand(
            "Ana", "Novak", "ana@example.org", "private", null, null, null, null, true, Set.of());

    service.register(student);

    ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(Registration.class);
    verify(repository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().isStudent()).isTrue();
    assertThat(captor.getValue().getGrossFee()).isEqualByComparingTo("0");
  }
}
