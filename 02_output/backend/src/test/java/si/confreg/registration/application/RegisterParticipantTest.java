package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import si.confreg.registration.domain.FeePolicy;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationNumbers;
import si.confreg.registration.domain.Workshop;
import si.confreg.registration.domain.WorkshopCatalog;

/** Unit tests of the registration use case with in-memory fakes; parameters are synthetic. */
class RegisterParticipantTest {

  private static final Instant NOW = Instant.parse("2031-05-05T10:00:00Z");

  /** Store that can simulate number collisions. */
  static final class FakeStore implements RegistrationStore {
    final Map<String, Registration> rows = new HashMap<>();
    int collisionsToSimulate;

    @Override
    public Registration add(Registration registration) {
      if (collisionsToSimulate > 0 || rows.containsKey(registration.registrationNumber())) {
        collisionsToSimulate--;
        throw new DuplicateRegistrationNumberException(new IllegalStateException("dup"));
      }
      rows.put(registration.registrationNumber(), registration);
      return registration;
    }

    @Override
    public Optional<Registration> findByNumber(String number) {
      return Optional.ofNullable(rows.get(number));
    }
  }

  static final class FakeSender implements ConfirmationSender {
    final List<Registration> sent = new ArrayList<>();
    RuntimeException failure;

    @Override
    public void sendConfirmation(Registration registration) {
      if (failure != null) {
        throw failure;
      }
      sent.add(registration);
    }
  }

  private final FakeStore store = new FakeStore();
  private final FakeSender sender = new FakeSender();
  private final FeePolicy feePolicy =
      new FeePolicy(
          ZoneOffset.UTC,
          LocalDate.of(2031, 5, 5),
          new BigDecimal("10.00"),
          new BigDecimal("20.00"),
          new BigDecimal("0.10"));
  private final RegisterParticipant useCase =
      new RegisterParticipant(
          new RegistrationValidator(new WorkshopCatalog(List.of(new Workshop("T1", "One")))),
          feePolicy,
          new RegistrationNumbers(),
          store,
          sender,
          () -> NOW);

  private static RegistrationRequest valid() {
    return new RegistrationRequest(
        "Ana", "Novak", "ana@example.org", "private", null, null, null, List.of("T1"), Set.of());
  }

  @Test
  void storesOneRegistrationAndSendsOneConfirmation() {
    RegisterParticipant.Outcome outcome = useCase.register(valid());

    assertThat(outcome).isInstanceOf(RegisterParticipant.Registered.class);
    Registration registration = ((RegisterParticipant.Registered) outcome).registration();
    assertThat(store.rows).containsOnlyKeys(registration.registrationNumber());
    assertThat(sender.sent).containsExactly(registration);
    assertThat(registration.submittedAt()).isEqualTo(NOW);
    assertThat(registration.fee()).isEqualTo(feePolicy.feeAt(NOW));
    assertThat(registration.workshopId()).isEqualTo("T1");
    assertThat(RegistrationNumbers.isWellFormed(registration.registrationNumber())).isTrue();
  }

  @Test
  void invalidRequestStoresNothingAndSendsNothing() {
    RegistrationRequest invalid =
        new RegistrationRequest(null, "N", "x", "private", null, null, null, null, Set.of());

    RegisterParticipant.Outcome outcome = useCase.register(invalid);

    assertThat(outcome).isInstanceOf(RegisterParticipant.Rejected.class);
    assertThat(((RegisterParticipant.Rejected) outcome).errors())
        .containsOnlyKeys("firstName", "email");
    assertThat(store.rows).isEmpty();
    assertThat(sender.sent).isEmpty();
  }

  @Test
  void failedConfirmationKeepsTheRegistration() {
    sender.failure = new IllegalStateException("smtp down");

    RegisterParticipant.Outcome outcome = useCase.register(valid());

    assertThat(outcome).isInstanceOf(RegisterParticipant.Registered.class);
    assertThat(store.rows).hasSize(1);
  }

  @Test
  void numberCollisionIsRetriedWithANewNumber() {
    store.collisionsToSimulate = RegisterParticipant.MAX_NUMBER_ATTEMPTS - 1;

    RegisterParticipant.Outcome outcome = useCase.register(valid());

    assertThat(outcome).isInstanceOf(RegisterParticipant.Registered.class);
    assertThat(store.rows).hasSize(1);
    assertThat(sender.sent).hasSize(1);
  }

  @Test
  void tooManyCollisionsFail() {
    store.collisionsToSimulate = RegisterParticipant.MAX_NUMBER_ATTEMPTS;

    assertThatThrownBy(() -> useCase.register(valid()))
        .isInstanceOf(DuplicateRegistrationNumberException.class);
    assertThat(sender.sent).isEmpty();
  }

  @Test
  void collidingNumberIsReplaced() {
    RandomGenerator zeros = () -> 0L;
    RegisterParticipant deterministic =
        new RegisterParticipant(
            new RegistrationValidator(new WorkshopCatalog(List.of())),
            feePolicy,
            new RegistrationNumbers(zeros),
            store,
            sender,
            () -> NOW);
    RegistrationRequest noWorkshop =
        new RegistrationRequest(
            "Ana", "Novak", "ana@example.org", "private", null, null, null, null, Set.of());

    assertThat(deterministic.register(noWorkshop))
        .isInstanceOf(RegisterParticipant.Registered.class);
    assertThatThrownBy(() -> deterministic.register(noWorkshop))
        .isInstanceOf(DuplicateRegistrationNumberException.class);
    assertThat(store.rows).hasSize(1);
  }

  @Test
  void queryFindsOnlyWellFormedStoredNumbers() {
    Registration stored =
        ((RegisterParticipant.Registered) useCase.register(valid())).registration();
    RegistrationQuery query = new RegistrationQuery(store);

    assertThat(query.find(stored.registrationNumber())).contains(stored);
    assertThat(query.find("REG-ZZZZZZZZZZ")).isEmpty();
    assertThat(query.find("../etc")).isEmpty();
    assertThat(query.find(null)).isEmpty();
  }
}
