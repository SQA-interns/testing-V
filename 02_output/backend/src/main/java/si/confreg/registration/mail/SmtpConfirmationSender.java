package si.confreg.registration.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.BusinessSettings;
import si.confreg.registration.application.ConfirmationFailedException;
import si.confreg.registration.application.ConfirmationSender;
import si.confreg.registration.domain.Registration;

/**
 * Sends the confirmation through the configured SMTP server only (AR-07). Headers are set through
 * the Jakarta Mail API, never by concatenation (SR-05).
 */
@Component
public final class SmtpConfirmationSender implements ConfirmationSender {

  private final JavaMailSender mailSender;
  private final BusinessSettings settings;
  private final String from;

  public SmtpConfirmationSender(
      JavaMailSender mailSender,
      BusinessSettings settings,
      @Value("${app.mail-from}") String from) {
    if (from == null || from.isBlank()) {
      throw new IllegalStateException("APP_MAIL_FROM must be set");
    }
    this.mailSender = mailSender;
    this.settings = settings;
    this.from = from.strip();
  }

  @Override
  public void send(Registration registration) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(registration.participant().email());
      helper.setSubject(ConfirmationText.subject(registration));
      helper.setText(ConfirmationText.body(registration, settings), false);
      mailSender.send(message);
    } catch (MailException | MessagingException e) {
      throw new ConfirmationFailedException(e);
    }
  }
}
