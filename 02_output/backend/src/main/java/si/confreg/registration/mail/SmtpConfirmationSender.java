package si.confreg.registration.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.confreg.registration.application.ConfirmationSender;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.domain.WorkshopCatalog;

/** Sends the confirmation through the configured SMTP server (AR-07), UTF-8 plain text. */
public final class SmtpConfirmationSender implements ConfirmationSender {

  private final JavaMailSender mailSender;
  private final InternetAddress from;
  private final ConfirmationMessage message;

  /**
   * @throws IllegalArgumentException if {@code fromAddress} is not a valid address
   */
  public SmtpConfirmationSender(
      JavaMailSender mailSender, String fromAddress, WorkshopCatalog workshops) {
    this.mailSender = mailSender;
    this.from = parse(fromAddress);
    this.message = new ConfirmationMessage(workshops);
  }

  @Override
  public void sendConfirmation(Registration registration) {
    MimeMessage mime = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(parse(registration.email()));
      helper.setSubject(message.subject(registration));
      helper.setText(message.body(registration), false);
    } catch (MessagingException e) {
      throw new MailPreparationException("confirmation could not be prepared", e);
    }
    mailSender.send(mime);
  }

  private static InternetAddress parse(String address) {
    try {
      return new InternetAddress(address, true);
    } catch (AddressException e) {
      throw new IllegalArgumentException("invalid e-mail address", e);
    }
  }
}
