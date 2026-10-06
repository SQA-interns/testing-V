package si.confreg.registration.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.confreg.registration.domain.Fee;
import si.confreg.registration.domain.Payer;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.Workshop;
import si.confreg.registration.domain.WorkshopCatalog;

/** Unit tests of the confirmation message (contract confirmation-email.json, SR-05, NFR-01). */
class SmtpConfirmationSenderTest {

  /** Captures messages instead of talking to an SMTP server. */
  static final class CapturingSender extends JavaMailSenderImpl {
    final List<MimeMessage> sent = new ArrayList<>();

    @Override
    public MimeMessage createMimeMessage() {
      return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Override
    public void send(MimeMessage message) {
      sent.add(message);
    }
  }

  private static final WorkshopCatalog WORKSHOPS =
      new WorkshopCatalog(List.of(new Workshop("T1", "Test workshop")));

  private static Registration registration(String workshop) {
    return new Registration(
        "REG-0123456789",
        "Čedomir",
        "Šušteršič",
        "cedo@example.org",
        Payer.privatePerson(),
        workshop,
        new Fee(new BigDecimal("10.00"), new BigDecimal("2.50"), new BigDecimal("12.50")),
        Instant.parse("2031-01-01T00:00:00Z"));
  }

  @Test
  void sendsOnePlainTextUtf8MessageAsInTheContract() throws Exception {
    CapturingSender mail = new CapturingSender();
    SmtpConfirmationSender sender =
        new SmtpConfirmationSender(mail, "noreply@test.example", WORKSHOPS);

    sender.sendConfirmation(registration("T1"));

    assertThat(mail.sent).hasSize(1);
    MimeMessage message = mail.sent.get(0);
    message.saveChanges();
    assertThat(message.getFrom()[0].toString()).isEqualTo("noreply@test.example");
    assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
        .isEqualTo("cedo@example.org");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation REG-0123456789");
    assertThat(message.getContentType()).contains("text/plain").containsIgnoringCase("utf-8");
    String body = (String) message.getContent();
    assertThat(body)
        .contains("Dear Čedomir Šušteršič,")
        .contains("Registration number: REG-0123456789")
        .contains("Workshop: Test workshop")
        .contains("Net fee: 10.00 EUR")
        .contains("VAT: 2.50 EUR")
        .contains("Gross fee: 12.50 EUR")
        .doesNotContain("<");
    ByteArrayOutputStream raw = new ByteArrayOutputStream();
    message.writeTo(raw);
    assertThat(raw.toString(StandardCharsets.US_ASCII)).doesNotContain("Content-Type: text/html");
  }

  @Test
  void noWorkshopAndUnknownWorkshopAreWritten() {
    ConfirmationMessage message = new ConfirmationMessage(WORKSHOPS);

    assertThat(message.body(registration(null))).contains("Workshop: none");
    assertThat(message.body(registration("GONE"))).contains("Workshop: GONE");
  }

  @Test
  void invalidAddressesAreRejected() {
    CapturingSender mail = new CapturingSender();
    assertThatThrownBy(() -> new SmtpConfirmationSender(mail, "not an address", WORKSHOPS))
        .isInstanceOf(IllegalArgumentException.class);

    SmtpConfirmationSender sender = new SmtpConfirmationSender(mail, "a@test.example", WORKSHOPS);
    Registration bad =
        new Registration(
            "REG-0123456789",
            "A",
            "B",
            "x@y.z\r\nBcc: evil@example.org",
            Payer.privatePerson(),
            null,
            new Fee(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE),
            Instant.EPOCH);
    assertThatThrownBy(() -> sender.sendConfirmation(bad))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(mail.sent).isEmpty();
  }
}
