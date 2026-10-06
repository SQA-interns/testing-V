package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import si.confreg.registration.config.ConferenceClock;
import si.confreg.registration.domain.FeeCalculator;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationNumberGenerator;
import si.confreg.registration.domain.RegistrationValidator;
import si.confreg.registration.domain.ValidationFailedException;
import si.confreg.registration.persistence.RegistrationEntity;
import si.confreg.registration.persistence.RegistrationRepository;
import si.confreg.registration.testsupport.TestSettings;

class RegistrationServiceTest {

  private static final Instant LATE = Instant.parse("2030-03-01T12:00:00Z");

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final RegistrationNumberGenerator numbers = mock(RegistrationNumberGenerator.class);
  private final ConferenceClock clock = mock(ConferenceClock.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private RegistrationService service;

  private static final Map<String, Object> BODY =
      Map.of(
          "firstName",
          "Ana",
          "lastName",
          "Novak",
          "email",
          "ana@example.org",
          "payerType",
          "private");

  @BeforeEach
  void setUp() {
    when(clock.now()).thenReturn(LATE);
    when(numbers.next()).thenReturn("CR-A", "CR-B", "CR-C", "CR-D");
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              RegistrationEntity entity = invocation.getArgument(0);
              ReflectionTestUtils.setField(entity, "id", 42L);
              return entity;
            });
    service =
        new RegistrationService(
            new RegistrationValidator(TestSettings.settings()),
            new FeeCalculator(TestSettings.settings()),
            numbers,
            repository,
            clock,
            events);
  }

  @Test
  void registerStoresWithFeeAtSubmissionTimeAndPublishesTheStoredEvent() {
    Registration registration = service.register(BODY);

    assertThat(registration.registrationNumber()).isEqualTo("CR-A");
    assertThat(registration.netFee()).isEqualByComparingTo(TestSettings.FEE_REGULAR);
    assertThat(registration.firstName()).isEqualTo("Ana");
    verify(events).publishEvent(new RegistrationStored(42L));
  }

  @Test
  void registerSkipsNumbersThatAreAlreadyUsed() {
    when(repository.existsByRegistrationNumber("CR-A")).thenReturn(true);
    when(repository.existsByRegistrationNumber("CR-B")).thenReturn(true);

    assertThat(service.register(BODY).registrationNumber()).isEqualTo("CR-C");
  }

  @Test
  void registerGivesUpAfterThreeUsedNumbers() {
    when(repository.existsByRegistrationNumber(any())).thenReturn(true);

    assertThatThrownBy(() -> service.register(BODY)).isInstanceOf(IllegalStateException.class);
    verify(repository, never()).save(any());
    verify(numbers, org.mockito.Mockito.times(3)).next();
  }

  @Test
  void invalidRequestStoresNothingAndPublishesNothing() {
    assertThatThrownBy(() -> service.register(Map.of()))
        .isInstanceOf(ValidationFailedException.class);
    verify(repository, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void findReturnsTheStoredRegistrationOrNothing() {
    RegistrationEntity entity = mock(RegistrationEntity.class);
    Registration registration = service.register(BODY);
    when(entity.toRegistration()).thenReturn(registration);
    when(repository.findByRegistrationNumber("CR-A")).thenReturn(Optional.of(entity));

    assertThat(service.find("CR-A")).contains(registration);
    assertThat(service.find("CR-NONE")).isEmpty();
  }
}
