package si.confreg.registration.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Price;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.Registration.NewRegistration;

class ConfirmationMessageTest {

  private static final Map<String, String> WORKSHOPS = Map.of("W1", "One");

  static Registration registration(
      PayerType payer, String vatId, String workshop, boolean student, String gross) {
    NewRegistration data =
        new NewRegistration(
            "Žiga",
            "Čebašek",
            "ziga@example.org",
            payer,
            payer == PayerType.COMPANY ? "Primer d.o.o." : null,
            payer == PayerType.COMPANY ? "Cesta 1" : null,
            vatId,
            workshop,
            student);
    BigDecimal grossFee = new BigDecimal(gross);
    Price price = new Price(Price.Tier.REGULAR, grossFee, BigDecimal.ZERO.setScale(2), grossFee);
    return Registration.create("CR-000007", data, price, Instant.parse("2026-08-01T10:00:00Z"));
  }

  @Test
  void payingPrivateParticipant() {
    ConfirmationMessage message =
        ConfirmationMessage.of(
            registration(PayerType.PRIVATE, null, "W1", false, "366.00"), WORKSHOPS);

    assertThat(message.subject()).isEqualTo("Registration confirmation CR-000007");
    assertThat(message.body())
        .contains("Dear Žiga Čebašek,")
        .contains("Registration number: CR-000007")
        .contains("Workshop: One (W1)")
        .contains("Payer: private")
        .contains("Total: 366.00 EUR")
        .contains("The invoice for 366.00 EUR will be sent to you separately")
        .doesNotContain("free of charge");
  }

  @Test
  void companyWithAndWithoutVatId() {
    assertThat(
            ConfirmationMessage.of(
                    registration(PayerType.COMPANY, "SI123", null, false, "1.00"), WORKSHOPS)
                .body())
        .contains("Payer: Primer d.o.o., Cesta 1, VAT ID SI123")
        .contains("Workshop: none");
    assertThat(
            ConfirmationMessage.of(
                    registration(PayerType.COMPANY, null, null, false, "1.00"), WORKSHOPS)
                .body())
        .contains("Payer: Primer d.o.o., Cesta 1\r\n");
  }

  @Test
  void studentIsToldItIsFree() {
    String body =
        ConfirmationMessage.of(registration(PayerType.PRIVATE, null, null, true, "0.00"), WORKSHOPS)
            .body();

    assertThat(body)
        .contains("free of charge")
        .contains("student status")
        .doesNotContain("invoice");
  }

  @Test
  void unknownWorkshopIdIsShownAsIs() {
    assertThat(
            ConfirmationMessage.of(
                    registration(PayerType.PRIVATE, null, "W9", false, "1.00"), WORKSHOPS)
                .body())
        .contains("Workshop: W9\r\n");
  }
}
