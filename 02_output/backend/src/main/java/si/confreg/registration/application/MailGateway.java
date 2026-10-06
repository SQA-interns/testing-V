package si.confreg.registration.application;

/** Sends one plain-text e-mail (AR-07: implemented only by the backend's mail component). */
public interface MailGateway {

  /**
   * Sends the message or throws {@link MailDeliveryException} when the SMTP server did not accept
   * it.
   */
  void send(String to, String subject, String plainText);
}
