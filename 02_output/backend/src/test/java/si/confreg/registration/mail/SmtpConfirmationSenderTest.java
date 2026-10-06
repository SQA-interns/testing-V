package si.confreg.registration.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.confreg.registration.application.BusinessSettings;
import si.confreg.registration.application.ConfirmationFailedException;
import si.confreg.registration.domain.Company;
import si.confreg.registration.domain.FeeSchedule;
import si.confreg.registration.domain.Fees;
import si.confreg.registration.domain.Participant;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.WorkshopCatalogue;

class SmtpConfirmationSenderTest {

  private final BusinessSettings settings =
      new BusinessSettings(
          new FeeSchedule(
              LocalDate.of(2026, 7, 31),
              ZoneId.of("Europe/Ljubljana"),
              new BigDecimal("240.00"),
              new BigDecimal("300.00")),
          new BigDecimal("0.22"),
          WorkshopCatalogue.parse("W1=Requirements engineering"));

  private static Registration registration(Company company, String workshop) {
    return new Registration(
        "REG-000007",
        new Participant(
            "Špela",
            "Žagar",
            "spela@example.com",
            company == null ? PayerType.PRIVATE : PayerType.COMPANY,
            company,
            workshop),
        Fees.fromNet(new BigDecimal("240.00"), new BigDecimal("0.22")),
        Instant.parse("2026-05-01T10:00:00Z"));
  }

  private MimeMessage sent(Registration registration) throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    new SmtpConfirmationSender(mailSender, settings, " registration@confreg.test ")
        .send(registration);
    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    MimeMessage message = captor.getValue();
    message.saveChanges();
    return message;
  }

  @Test
  void sendsPlainTextUtf8ConfirmationWithNumberAndAmounts() throws Exception {
    MimeMessage message = sent(registration(null, "W1"));

    assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
        .isEqualTo("spela@example.com");
    assertThat(message.getFrom()[0].toString()).isEqualTo("registration@confreg.test");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation REG-000007");
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    String body = (String) message.getContent();
    assertThat(body)
        .contains("Dear Špela Žagar,")
        .contains("Registration number: REG-000007")
        .contains("Workshop: Requirements engineering")
        .contains("Fee (net): 240.00 EUR")
        .contains("VAT (22 %): 52.80 EUR")
        .contains("Total (gross): 292.80 EUR")
        .contains("Payer: private");
  }

  @Test
  void companyPayerAndNoWorkshop() throws Exception {
    MimeMessage message =
        sent(registration(new Company("Podjetje d.o.o.", "Ljubljana", "SI12345678"), null));

    String body = (String) message.getContent();
    assertThat(body)
        .contains("Workshop: none")
        .contains("Payer: Podjetje d.o.o., Ljubljana, VAT ID SI12345678");
  }

  @Test
  void markupInNamesStaysPlainText() throws Exception {
    MimeMessage message = sent(registration(new Company("<b>X</b>", "A", "SI1"), null));
    assertThat(message.getContentType()).startsWith("text/plain");
    assertThat(message.getHeader("Content-Type")[0]).doesNotContainIgnoringCase("html");
  }

  @Test
  void smtpFailureBecomesConfirmationFailed() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("refused")).when(mailSender).send(any(MimeMessage.class));

    assertThatThrownBy(
            () ->
                new SmtpConfirmationSender(mailSender, settings, "a@b.com")
                    .send(registration(null, null)))
        .isInstanceOf(ConfirmationFailedException.class)
        .hasCauseInstanceOf(MailSendException.class);
  }

  @Test
  void senderAddressIsRequired() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    assertThatThrownBy(() -> new SmtpConfirmationSender(mailSender, settings, " "))
        .isInstanceOf(IllegalStateException.class);
  }
}
