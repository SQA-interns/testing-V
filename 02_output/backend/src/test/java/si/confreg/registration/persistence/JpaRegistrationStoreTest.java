package si.confreg.registration.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import si.confreg.registration.domain.Company;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Participant;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

class JpaRegistrationStoreTest {

  private final RegistrationJpaRepository repository = mock(RegistrationJpaRepository.class);
  private final JpaRegistrationStore store = new JpaRegistrationStore(repository);

  private static Registration company() {
    return new Registration(
        "REG-000123",
        new Participant(
            "Matej",
            "Šuštar",
            "m@example.com",
            PayerType.COMPANY,
            new Company("Podjetje", "Ljubljana", "SI1"),
            "W2"),
        Fees.fromNet(new BigDecimal("300.00"), new BigDecimal("0.22")),
        Instant.parse("2026-09-01T10:00:00Z"));
  }

  @Test
  void formatsRegistrationNumberWithSixDigitsAtLeast() {
    when(repository.nextRegistrationSequence()).thenReturn(7L, 1_234_567L);

    assertThat(store.nextRegistrationNumber()).isEqualTo("REG-000007");
    assertThat(store.nextRegistrationNumber()).isEqualTo("REG-1234567");
  }

  @Test
  void savedRowRoundTripsToTheSameRegistration() {
    Registration registration = company();
    store.save(registration);
    ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    RegistrationEntity row = captor.getValue();
    assertThat(row.payerType()).isEqualTo("company");
    assertThat(row.companyVatId()).isEqualTo("SI1");
    when(repository.findByRegistrationNumber("REG-000123")).thenReturn(Optional.of(row));

    assertThat(store.find("REG-000123")).contains(registration);
  }

  @Test
  void privateRowHasNoCompany() {
    Registration registration =
        new Registration(
            "REG-000001",
            new Participant("Ana", "Novak", "a@example.com", PayerType.PRIVATE, null, null),
            Fees.fromNet(new BigDecimal("240.00"), new BigDecimal("0.22")),
            Instant.parse("2026-05-01T10:00:00Z"));
    store.save(registration);
    ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
    verify(repository).saveAndFlush(captor.capture());
    when(repository.findByRegistrationNumber("REG-000001"))
        .thenReturn(Optional.of(captor.getValue()));

    assertThat(store.find("REG-000001")).contains(registration);
    assertThat(captor.getValue().companyName()).isNull();
  }

  @Test
  void unknownNumberIsEmpty() {
    when(repository.findByRegistrationNumber("REG-999999")).thenReturn(Optional.empty());
    assertThat(store.find("REG-999999")).isEmpty();
  }
}
