package si.confreg.registration.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.testsupport.TestSettings;

/** Confirmation content (AC-001-04 contract, SR-05, NFR-01). */
class ConfirmationMailerTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final ConfirmationMailer mailer = new ConfirmationMailer(sender, TestSettings.settings());

  private static Registration registration(PayerType payer, String workshop) {
    boolean company = payer == PayerType.COMPANY;
    return new Registration(
        "CR-ABCDEFGH12",
        "Žiga",
        "Čašič",
        "ziga@example.org",
        payer,
        company ? "Šola d.o.o." : null,
        company ? "Koroška cesta 1, 2000 Maribor" : null,
        company ? "SI00000001" : null,
        workshop,
        new BigDecimal("150.50"),
        new BigDecimal("38.38"),
        new BigDecimal("188.88"));
  }

  @Test
  void sendsOnePlainTextMessageToTheParticipantOnly() {
    mailer.send(registration(PayerType.PRIVATE, null));

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(captor.capture());
    SimpleMailMessage message = captor.getValue();
    assertThat(message.getTo()).containsExactly("ziga@example.org");
    assertThat(message.getCc()).isNull();
    assertThat(message.getBcc()).isNull();
    assertThat(message.getFrom()).isEqualTo("sender@example.test");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation CR-ABCDEFGH12");
    assertThat(message.getText()).isEqualTo(mailer.body(registration(PayerType.PRIVATE, null)));
  }

  @Test
  void bodyStatesNumberAndAllAmountsWithTwoDecimals() {
    String body = mailer.body(registration(PayerType.PRIVATE, null));
    assertThat(body)
        .contains("Registration number: CR-ABCDEFGH12")
        .contains("Net fee: 150.50 EUR")
        .contains("VAT (25.5 %): 38.38 EUR")
        .contains("Gross fee: 188.88 EUR")
        .contains("Payer: private person")
        .contains("Workshop: none");
  }

  @Test
  void bodyKeepsSlovenianCharactersAndCompanyData() {
    String body = mailer.body(registration(PayerType.COMPANY, "WB"));
    assertThat(body)
        .contains("Dear Žiga Čašič,")
        .contains("Payer: Šola d.o.o., Koroška cesta 1, 2000 Maribor, VAT ID SI00000001")
        .contains("Workshop: Beta workshop");
  }

  @Test
  void unknownWorkshopIdIsShownAsIs() {
    assertThat(mailer.body(registration(PayerType.PRIVATE, "OLD"))).contains("Workshop: OLD");
  }

  @Test
  void bodyHasNoMarkup() {
    mailer.send(registration(PayerType.PRIVATE, null));
    verify(sender).send(any(SimpleMailMessage.class));
    assertThat(mailer.body(registration(PayerType.PRIVATE, null))).doesNotContain("<");
  }
}
