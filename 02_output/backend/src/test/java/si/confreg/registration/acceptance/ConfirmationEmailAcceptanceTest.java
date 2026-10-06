package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Mailpit;
import si.confreg.registration.acceptance.support.Registrations;

/** US-001 [4]: the confirmation e-mail, as received by the mail catcher. */
class ConfirmationEmailAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac001_04_oneConfirmationWithNumberNameWorkshopAndAmounts() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.privatePerson(), "workshops", List.of(AcceptanceConfig.WORKSHOP_A));
    String email = (String) body.get("email");

    ApiClient.Response created = registerOk(body, AcceptanceConfig.earlyBirdTime());
    String number = (String) created.json().get("registrationNumber");

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.subject()).contains(number);
    assertThat(message.text())
        .contains(number)
        .contains((String) body.get("firstName"))
        .contains((String) body.get("lastName"))
        .contains(AcceptanceConfig.WORKSHOP_A)
        .contains(created.rawAmount("netFee"))
        .contains(created.rawAmount("vat"))
        .contains(created.rawAmount("grossFee"));
    assertThat(mailpit().countTo(email, SETTLE)).isEqualTo(1);
  }

  @Test
  void ac001_04_sr05_confirmationIsPlainTextUtf8() {
    Map<String, Object> body = Registrations.privatePerson();
    body.put("firstName", "<b>Ana</b>");
    String email = (String) body.get("email");

    registerOk(body, AcceptanceConfig.earlyBirdTime());

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.contentType()).startsWith("text/plain").containsIgnoringCase("utf-8");
    assertThat(message.html()).isNullOrEmpty();
    assertThat(message.text()).contains("<b>Ana</b>");
  }

  @Test
  void ac001_04_nfr01_slovenianCharactersSurviveInEmail() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.privatePerson(), "workshops", List.of(AcceptanceConfig.WORKSHOP_B));
    body.put("firstName", "Žiga");
    body.put("lastName", "Čebašek Šuštar");
    String email = (String) body.get("email");

    registerOk(body, AcceptanceConfig.earlyBirdTime());

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.text()).contains("Žiga").contains("Čebašek Šuštar");
  }

  @Test
  void ac001_04_noWorkshopIsStatedAsNone() {
    Map<String, Object> body = Registrations.privatePerson();
    String email = (String) body.get("email");

    registerOk(body, AcceptanceConfig.earlyBirdTime());

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.text()).containsIgnoringCase("workshop: none");
  }

  @Test
  void ac001_25_payingParticipantIsToldAmountAndThatInvoiceFollowsSeparately() {
    Map<String, Object> body = Registrations.company();
    String email = (String) body.get("email");

    ApiClient.Response created = registerOk(body, AcceptanceConfig.regularTime());

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.text())
        .contains(created.rawAmount("grossFee"))
        .containsIgnoringCase("invoice")
        .containsIgnoringCase("separately")
        .containsIgnoringCase("accounting");
  }

  @Test
  void ac001_26_studentIsToldParticipationIsFreeAndStatusMayBeChecked() {
    Map<String, Object> body = Registrations.with(Registrations.privatePerson(), "student", true);
    String email = (String) body.get("email");

    registerOk(body, AcceptanceConfig.regularTime());

    Mailpit.Message message = mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    assertThat(message.text())
        .containsIgnoringCase("free of charge")
        .containsIgnoringCase("student status");
  }
}
