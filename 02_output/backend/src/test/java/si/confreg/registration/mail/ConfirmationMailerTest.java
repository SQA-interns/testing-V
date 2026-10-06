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
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.confreg.registration.config.AppProperties;
import si.confreg.registration.domain.Price;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.RegistrationInput;

class ConfirmationMailerTest {

  private static final AppProperties PROPERTIES =
      new AppProperties(
          "UTC",
          "2030-01-31",
          new BigDecimal("10.00"),
          new BigDecimal("20.00"),
          new BigDecimal("0.095"),
          "K1=Kappa workshop;K2=Lambda",
          5,
          "disabled",
          "sender@example.com",
          false,
          false);

  private static final Price PRICE =
      new Price(new BigDecimal("9.13"), new BigDecimal("0.87"), new BigDecimal("10.00"));

  private static Registration company() {
    return Registration.create(
        "REG-000007",
        new RegistrationInput(
            "Žiga",
            "Šuštar",
            "ziga@example.com",
            "company",
            "Čebelica d.o.o.",
            "Čopova 1\nLjubljana",
            "SI123",
            List.of("K1")),
        PRICE,
        Instant.EPOCH);
  }

  private static Registration privatePayer(String lastName) {
    return Registration.create(
        "REG-000008",
        new RegistrationInput(
            "Ana", lastName, "ana@example.com", "private", null, null, null, List.of()),
        PRICE,
        Instant.EPOCH);
  }

  @Test
  void contentContainsNumberAmountsWorkshopAndCompanyPayer() {
    ConfirmationEmail email =
        ConfirmationEmail.of(company(), "Kappa workshop", PROPERTIES.vatRate());

    assertThat(email.subject()).isEqualTo("Registration confirmation REG-000007");
    assertThat(email.body())
        .startsWith("Dear Žiga Šuštar,")
        .contains("Registration number: REG-000007")
        .contains("Workshop: K1 - Kappa workshop")
        .contains("Fee: 10.00 EUR (net 9.13 EUR + VAT 9.5% 0.87 EUR)")
        .contains("Payer: Čebelica d.o.o., Čopova 1\nLjubljana, VAT ID SI123");
  }

  @Test
  void privatePayerWithoutWorkshop() {
    ConfirmationEmail email =
        ConfirmationEmail.of(privatePayer("Kovač"), null, new BigDecimal("0.22"));

    assertThat(email.body())
        .contains("Workshop: none")
        .contains("VAT 22% 0.87 EUR")
        .contains("Payer: Ana Kovač");
  }

  @Test
  void sendsPlainTextUtf8MessageToParticipant() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

    new ConfirmationMailer(sender, PROPERTIES).sendConfirmation(company());

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(sent.capture());
    MimeMessage message = sent.getValue();
    message.saveChanges();
    assertThat(message.getFrom()[0].toString()).isEqualTo("sender@example.com");
    assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
        .isEqualTo("ziga@example.com");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation REG-000007");
    assertThat(message.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    assertThat((String) message.getContent()).contains("Čebelica d.o.o.");
  }

  @Test
  void headerInjectionInNameStaysInBody() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

    new ConfirmationMailer(sender, PROPERTIES)
        .sendConfirmation(privatePayer("Kovač\r\nBcc: victim@example.com"));

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(sent.capture());
    assertThat(sent.getValue().getRecipients(Message.RecipientType.BCC)).isNull();
    assertThat(sent.getValue().getSubject()).doesNotContain("Bcc");
  }

  @Test
  void smtpFailureIsReportedAsNotSent() {
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("refused")).when(sender).send(any(MimeMessage.class));

    assertThatThrownBy(() -> new ConfirmationMailer(sender, PROPERTIES).sendConfirmation(company()))
        .isInstanceOf(ConfirmationNotSentException.class)
        .hasCauseInstanceOf(MailSendException.class);
  }
}
