package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import si.confreg.registration.domain.FeeSchedule;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationInput;
import si.confreg.registration.domain.WorkshopCatalogue;

class RegisterParticipantTest {

  private static final Instant EARLY = Instant.parse("2026-05-01T10:00:00Z");
  private static final Instant LATE = Instant.parse("2026-09-01T10:00:00Z");

  private final BusinessSettings settings =
      new BusinessSettings(
          new FeeSchedule(
              LocalDate.of(2026, 7, 31),
              ZoneId.of("Europe/Ljubljana"),
              new BigDecimal("240.00"),
              new BigDecimal("300.00")),
          new BigDecimal("0.22"),
          WorkshopCatalogue.parse("W1=One"));
  private final RegistrationStore store = mock(RegistrationStore.class);
  private final ConfirmationSender sender = mock(ConfirmationSender.class);
  private final TimeSource time = mock(TimeSource.class);
  private final RegisterParticipant useCase =
      new RegisterParticipant(settings, store, sender, time);

  private static RegistrationInput valid() {
    return new RegistrationInput(
        "Ana", "Novak", "ana@example.com", "private", null, null, null, List.of("W1"));
  }

  @Test
  void registersWithEarlyFeeStoresThenSends() {
    when(time.now()).thenReturn(EARLY);
    when(store.nextRegistrationNumber()).thenReturn("REG-000042");

    Registration registration = useCase.register(valid(), List.of());

    assertThat(registration.registrationNumber()).isEqualTo("REG-000042");
    assertThat(registration.createdAt()).isEqualTo(EARLY);
    assertThat(registration.fees().netFee()).isEqualByComparingTo("240.00");
    assertThat(registration.fees().vat()).isEqualByComparingTo("52.80");
    assertThat(registration.fees().grossFee()).isEqualByComparingTo("292.80");
    assertThat(registration.participant().workshopId()).isEqualTo("W1");
    InOrder order = inOrder(store, sender);
    order.verify(store).save(registration);
    order.verify(sender).send(registration);
  }

  @Test
  void usesRegularFeeAfterDeadline() {
    when(time.now()).thenReturn(LATE);
    when(store.nextRegistrationNumber()).thenReturn("REG-000001");

    assertThat(useCase.register(valid(), List.of()).fees().netFee()).isEqualByComparingTo("300.00");
  }

  @Test
  void rejectedRequestIsNotStoredOrSentAndUsesNoNumber() {
    RegistrationInput invalid =
        new RegistrationInput("Ana", "Novak", "bad", "private", null, null, null, null);

    assertThatThrownBy(() -> useCase.register(invalid, List.of()))
        .isInstanceOf(RegistrationRejectedException.class)
        .extracting(e -> ((RegistrationRejectedException) e).invalidFields())
        .isEqualTo(List.of("email"));
    verify(store, never()).nextRegistrationNumber();
    verify(store, never()).save(any());
    verify(sender, never()).send(any());
  }

  @Test
  void unacceptedFieldsFromApiRejectOtherwiseValidRequest() {
    assertThatThrownBy(() -> useCase.register(valid(), List.of("discountCode")))
        .isInstanceOf(RegistrationRejectedException.class)
        .extracting(e -> ((RegistrationRejectedException) e).invalidFields())
        .isEqualTo(List.of("discountCode"));
    verify(store, never()).save(any());
  }

  @Test
  void unacceptedAndInvalidFieldsAreCombinedWithoutDuplicates() {
    RegistrationInput invalid =
        new RegistrationInput("Ana", "Novak", "bad", "private", null, null, null, null);

    assertThatThrownBy(() -> useCase.register(invalid, List.of("email", "extra")))
        .extracting(e -> ((RegistrationRejectedException) e).invalidFields())
        .isEqualTo(List.of("email", "extra"));
  }

  @Test
  void mailFailurePropagatesSoTheTransactionRollsBack() {
    when(time.now()).thenReturn(EARLY);
    when(store.nextRegistrationNumber()).thenReturn("REG-000001");
    doThrow(new ConfirmationFailedException(new RuntimeException("smtp"))).when(sender).send(any());

    assertThatThrownBy(() -> useCase.register(valid(), List.of()))
        .isInstanceOf(ConfirmationFailedException.class);
  }

  @Test
  void findDelegatesToStore() {
    FindRegistration find = new FindRegistration(store);
    when(store.find("REG-000001")).thenReturn(Optional.empty());

    assertThat(find.find("REG-000001")).isEmpty();
  }
}
