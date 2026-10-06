package si.confreg.registration.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.confreg.registration.application.AppProperties;
import si.confreg.registration.application.MailDeliveryException;
import si.confreg.registration.application.MailGateway;

/** The backend's only mail component (AR-07): plain text over the configured SMTP server. */
@Component
public class SmtpMailGateway implements MailGateway {

  private static final String TIMEOUT_MS = "10000";

  private final JavaMailSenderImpl sender = new JavaMailSenderImpl();
  private final String from;

  public SmtpMailGateway(
      AppProperties properties,
      @Value("${SMTP_USERNAME:}") String username,
      @Value("${SMTP_PASSWORD:}") String password) {
    this.from = properties.mailFrom();
    sender.setHost(properties.smtpHost());
    sender.setPort(properties.smtpPort());
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    Properties javaMail = sender.getJavaMailProperties();
    javaMail.setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS);
    javaMail.setProperty("mail.smtp.timeout", TIMEOUT_MS);
    javaMail.setProperty("mail.smtp.writetimeout", TIMEOUT_MS);
    if (properties.smtpTls()) {
      javaMail.setProperty("mail.smtp.starttls.enable", "true");
      javaMail.setProperty("mail.smtp.starttls.required", "true");
    }
    if (!username.isBlank()) {
      sender.setUsername(username);
      sender.setPassword(password);
      javaMail.setProperty("mail.smtp.auth", "true");
    }
  }

  @Override
  public void send(String to, String subject, String plainText) {
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(plainText, false);
      sender.send(message);
    } catch (MailException | MessagingException e) {
      throw new MailDeliveryException(e);
    }
  }
}
