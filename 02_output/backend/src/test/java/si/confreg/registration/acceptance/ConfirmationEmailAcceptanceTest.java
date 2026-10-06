package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US-001 AC-001-03: confirmation e-mail with registration number and fee (also NFR-01). */
class ConfirmationEmailAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_001_03_participantReceivesEmailWithRegistrationNumberAndFee() {
    String email = uniqueEmail("mail");
    HttpResponse<String> response =
        postRegistration(privateRegistration(email), deadlineDayStart());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    String number = (String) json(response, "$.registrationNumber");
    BigDecimal net = config("APP_FEE_EARLY");
    BigDecimal gross = net.add(expectedVat(net));

    Mail mail = awaitSingleMailTo(email);

    assertThat(mail.to()).isEqualTo(email);
    assertThat(mail.subject()).contains(number);
    assertThat(mail.text()).contains(number);
    assertThat(mail.text()).contains(net.setScale(2).toPlainString());
    assertThat(mail.text()).contains(gross.setScale(2).toPlainString());
  }

  @Test
  void AC_001_03_regularFeeAppearsInEmailAfterDeadline() {
    String email = uniqueEmail("mail-regular");
    HttpResponse<String> response = postRegistration(privateRegistration(email), afterDeadline());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    BigDecimal net = config("APP_FEE_REGULAR");

    Mail mail = awaitSingleMailTo(email);

    assertThat(mail.text()).contains(net.setScale(2).toPlainString());
    assertThat(mail.text()).contains(net.add(expectedVat(net)).setScale(2).toPlainString());
  }

  @Test
  void AC_001_03_emailKeepsSlovenianCharactersOfTheNameUnchanged() {
    String email = uniqueEmail("mail-nfr01");
    Map<String, Object> body = privateRegistration(email);
    HttpResponse<String> response = postRegistration(body, deadlineDayStart());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);

    Mail mail = awaitSingleMailTo(email);

    assertThat(mail.text()).contains((String) body.get("firstName"));
    assertThat(mail.text()).contains((String) body.get("lastName"));
  }

  @Test
  void AC_001_03_exactlyOneEmailIsSentPerRegistration() {
    String email = uniqueEmail("mail-once");
    HttpResponse<String> response =
        postRegistration(privateRegistration(email), deadlineDayStart());
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);

    awaitSingleMailTo(email);

    assertThat(mailIdsTo(email)).hasSize(1);
  }
}
