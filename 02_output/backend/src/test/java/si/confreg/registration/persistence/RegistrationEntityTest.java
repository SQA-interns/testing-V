package si.confreg.registration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationData;

class RegistrationEntityTest {

  private static final RegistrationData COMPANY =
      new RegistrationData(
          "Ana",
          "Novak",
          "ana@example.org",
          PayerType.COMPANY,
          "Primer d.o.o.",
          "Koroška cesta 1, 2000 Maribor",
          "SI00000001",
          "WA");
  private static final Fees FEES =
      new Fees(new BigDecimal("100.00"), new BigDecimal("25.50"), new BigDecimal("125.50"));

  @Test
  void newRegistrationIsPendingWithNoAttemptsAndMapsEveryField() {
    RegistrationEntity entity =
        new RegistrationEntity("CR-1", COMPANY, FEES, Instant.parse("2030-01-01T00:00:00Z"));

    assertThat(entity.confirmationStatus()).isEqualTo(ConfirmationStatus.PENDING);
    assertThat(entity.confirmationAttempts()).isZero();
    assertThat(entity.toRegistration())
        .isEqualTo(
            new Registration(
                "CR-1",
                "Ana",
                "Novak",
                "ana@example.org",
                PayerType.COMPANY,
                "Primer d.o.o.",
                "Koroška cesta 1, 2000 Maribor",
                "SI00000001",
                "WA",
                FEES.netFee(),
                FEES.vat(),
                FEES.grossFee()));
  }

  @Test
  void attemptsAndStatusReflectTheStoredValues() {
    RegistrationEntity entity =
        new RegistrationEntity("CR-2", COMPANY, FEES, Instant.parse("2030-01-01T00:00:00Z"));
    ReflectionTestUtils.setField(entity, "confirmationAttempts", 3);
    ReflectionTestUtils.setField(entity, "confirmationStatus", ConfirmationStatus.SENT);
    ReflectionTestUtils.setField(entity, "id", 9L);

    assertThat(entity.confirmationAttempts()).isEqualTo(3);
    assertThat(entity.confirmationStatus()).isEqualTo(ConfirmationStatus.SENT);
    assertThat(entity.id()).isEqualTo(9L);
  }
}
