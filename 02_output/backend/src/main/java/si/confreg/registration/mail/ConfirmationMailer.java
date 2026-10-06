package si.confreg.registration.mail;

import java.math.BigDecimal;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import si.confreg.registration.config.AppSettings;
import si.confreg.registration.domain.PayerType;
import si.confreg.registration.domain.Registration;

/**
 * The only component that sends e-mail (AR-07). Plain-text UTF-8 confirmation per the contract
 * {@code confirmation-email.yaml}; user input only in the body and as the validated recipient
 * (SR-05).
 */
@Component
public class ConfirmationMailer {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final JavaMailSender sender;
  private final AppSettings settings;

  public ConfirmationMailer(JavaMailSender sender, AppSettings settings) {
    this.sender = sender;
    this.settings = settings;
  }

  /**
   * Sends the confirmation for one stored registration.
   *
   * @throws org.springframework.mail.MailException if the SMTP server does not accept it
   */
  public void send(Registration registration) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(settings.mailFrom());
    message.setTo(registration.email());
    message.setSubject("Registration confirmation " + registration.registrationNumber());
    message.setText(body(registration));
    sender.send(message);
  }

  String body(Registration registration) {
    String vatPercent = settings.vatRate().multiply(HUNDRED).stripTrailingZeros().toPlainString();
    return String.join(
        "\n",
        "Dear " + registration.firstName() + " " + registration.lastName() + ",",
        "",
        "thank you for registering for the conference.",
        "",
        "Registration number: " + registration.registrationNumber(),
        "Payer: " + payer(registration),
        "Workshop: " + workshop(registration),
        "",
        "Net fee: " + amount(registration.netFee()) + " EUR",
        "VAT (" + vatPercent + " %): " + amount(registration.vat()) + " EUR",
        "Gross fee: " + amount(registration.grossFee()) + " EUR",
        "",
        "The invoice will be issued after the conference.",
        "");
  }

  private static String payer(Registration registration) {
    if (registration.payerType() == PayerType.COMPANY) {
      return registration.companyName()
          + ", "
          + registration.companyAddress()
          + ", VAT ID "
          + registration.companyVatId();
    }
    return "private person";
  }

  private String workshop(Registration registration) {
    if (registration.workshop() == null) {
      return "none";
    }
    return settings.workshopTitles().getOrDefault(registration.workshop(), registration.workshop());
  }

  private static String amount(BigDecimal value) {
    return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
  }
}
