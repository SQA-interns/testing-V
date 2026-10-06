package si.confreg.registration.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.confreg.registration.config.AppProperties;
import si.confreg.registration.domain.Registration;

/**
 * Sends the confirmation e-mail through the configured SMTP server; the only mail sender (AR-07).
 */
@Component
public class ConfirmationMailer {

  private final JavaMailSender sender;
  private final AppProperties properties;

  public ConfirmationMailer(JavaMailSender sender, AppProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  /**
   * Hands the confirmation for {@code registration} to the SMTP server.
   *
   * @throws ConfirmationNotSentException if the message cannot be built or is not accepted
   */
  public void sendConfirmation(Registration registration) {
    String workshopName =
        registration.getWorkshop() == null
            ? null
            : properties.workshopCatalogue().get(registration.getWorkshop());
    ConfirmationEmail email =
        ConfirmationEmail.of(registration, workshopName, properties.vatRate());
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(properties.mailFrom());
      helper.setTo(registration.getEmail());
      helper.setSubject(email.subject());
      helper.setText(email.body(), false);
      sender.send(message);
    } catch (MessagingException | MailException e) {
      throw new ConfirmationNotSentException(e);
    }
  }
}
