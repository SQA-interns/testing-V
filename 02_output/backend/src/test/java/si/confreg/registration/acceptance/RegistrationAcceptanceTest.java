package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.confreg.registration.acceptance.support.AcceptanceConfig.earlyBirdTime;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Registrations;

/** US-001: storing a registration through the fixed API. */
class RegistrationAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac001_01_validRegistrationIsStoredAndReturnedWithNumberAndAmounts() {
    Map<String, Object> body = Registrations.privatePerson();

    ApiClient.Response created = registerOk(body, earlyBirdTime());

    Map<String, Object> json = created.json();
    String number = (String) json.get("registrationNumber");
    assertThat(number).isNotBlank();
    assertThat(created.header("Location")).endsWith("/api/registrations/" + number);
    assertThat(json)
        .containsEntry("firstName", body.get("firstName"))
        .containsEntry("lastName", body.get("lastName"))
        .containsEntry("email", body.get("email"))
        .containsEntry("payerType", "private")
        .containsEntry("companyName", null)
        .containsEntry("companyAddress", null)
        .containsEntry("companyVatId", null)
        .containsEntry("workshop", null);
    assertThat(created.amount("netFee")).isNotNull();
    assertThat(created.amount("vat")).isNotNull();
    assertThat(created.amount("grossFee")).isNotNull();

    ApiClient.Response stored = api().getAsOrganizer("/api/registrations/" + number);
    assertThat(stored.status()).isEqualTo(200);
    assertThat(stored.json()).containsEntry("email", body.get("email"));
  }

  @Test
  void ac001_01_nfr01_slovenianCharactersSurviveStorage() {
    Map<String, Object> body = Registrations.company();
    body.put("firstName", "Žiga");
    body.put("lastName", "Čebašek Šuštar");
    body.put("companyName", "Čistilnica Žužemberk d.o.o.");
    body.put("companyAddress", "Šmartinska cesta 5\n1000 Ljubljana");

    String number = (String) registerOk(body, earlyBirdTime()).json().get("registrationNumber");

    Map<String, Object> stored = api().getAsOrganizer("/api/registrations/" + number).json();
    assertThat(stored)
        .containsEntry("firstName", "Žiga")
        .containsEntry("lastName", "Čebašek Šuštar")
        .containsEntry("companyName", "Čistilnica Žužemberk d.o.o.")
        .containsEntry("companyAddress", "Šmartinska cesta 5\n1000 Ljubljana");
  }

  @Test
  void ac001_01_registrationNumbersAreUnique() {
    String first =
        (String)
            registerOk(Registrations.privatePerson(), earlyBirdTime())
                .json()
                .get("registrationNumber");
    String second =
        (String)
            registerOk(Registrations.privatePerson(), earlyBirdTime())
                .json()
                .get("registrationNumber");

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void ac001_16_noWorkshopIsStoredAsNull() {
    Map<String, Object> emptyList = Registrations.privatePerson();
    Map<String, Object> missing = Registrations.without(Registrations.privatePerson(), "workshops");

    assertThat(registerOk(emptyList, earlyBirdTime()).json()).containsEntry("workshop", null);
    assertThat(registerOk(missing, earlyBirdTime()).json()).containsEntry("workshop", null);
  }

  @Test
  void ac001_16_oneConfiguredWorkshopIsStored() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.privatePerson(), "workshops", List.of(AcceptanceConfig.WORKSHOP_B));

    ApiClient.Response created = registerOk(body, earlyBirdTime());
    String number = (String) created.json().get("registrationNumber");

    assertThat(created.json()).containsEntry("workshop", AcceptanceConfig.WORKSHOP_B);
    assertThat(api().getAsOrganizer("/api/registrations/" + number).json())
        .containsEntry("workshop", AcceptanceConfig.WORKSHOP_B);
  }

  @Test
  void ac001_18_secondRegistrationWithSameEmailIsRefusedWith409() {
    Map<String, Object> first = Registrations.privatePerson();
    String email = (String) first.get("email");
    registerOk(first, earlyBirdTime());
    mailpit().awaitMessageTo(email, MAIL_TIMEOUT);
    int before = storedCount();

    Map<String, Object> again =
        Registrations.with(
            Registrations.privatePerson(), "email", "  " + email.toUpperCase() + " ");
    ApiClient.Response response = api().register(again, earlyBirdTime());

    assertThat(response.status()).isEqualTo(409);
    assertThat(storedCount()).isEqualTo(before);
    assertThat(mailpit().countTo(email, SETTLE)).isEqualTo(1);
  }
}
