package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Registrations;

/** US-001 [4], D-14: a registration survives an SMTP outage and is confirmed afterwards. */
class MailOutageAcceptanceTest extends AcceptanceTestBase {

  /** The specification retries every 30 s; allow several rounds. */
  private static final Duration RETRY_TIMEOUT = Duration.ofSeconds(120);

  @AfterEach
  void smtpBackOn() {
    SMTP.on();
  }

  @Test
  void ac001_27_registrationIsStoredDuringSmtpOutageAndConfirmedWhenSmtpReturns() {
    Map<String, Object> body = Registrations.privatePerson();
    String email = (String) body.get("email");
    SMTP.off();

    ApiClient.Response created = registerOk(body, AcceptanceConfig.earlyBirdTime());
    String number = (String) created.json().get("registrationNumber");

    assertThat(api().getAsOrganizer("/api/registrations/" + number).status()).isEqualTo(200);
    assertThat(mailpit().countTo(email, SETTLE)).isZero();

    SMTP.on();

    assertThat(mailpit().awaitMessageTo(email, RETRY_TIMEOUT).text()).contains(number);
    assertThat(mailpit().countTo(email, SETTLE)).isEqualTo(1);
  }
}
