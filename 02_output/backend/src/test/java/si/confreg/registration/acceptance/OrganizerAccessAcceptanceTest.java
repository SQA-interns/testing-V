package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.acceptance.support.AcceptanceConfig;
import si.confreg.registration.acceptance.support.AcceptanceTestBase;
import si.confreg.registration.acceptance.support.ApiClient;
import si.confreg.registration.acceptance.support.Registrations;

/** US-001 [5]: organizer access to stored registrations and the invoicing export. */
class OrganizerAccessAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> EXPORT_COLUMNS =
      List.of(
          "Registration number",
          "Registered at",
          "First name",
          "Last name",
          "E-mail",
          "Payer type",
          "Company name",
          "Company address",
          "Company VAT ID",
          "Workshop",
          "Student",
          "Net fee",
          "VAT",
          "Gross fee");

  @Test
  void ac001_05_exportHasOneRowWithEveryInvoiceFieldPerRegistration() {
    Map<String, Object> body =
        Registrations.with(
            Registrations.company(), "workshops", List.of(AcceptanceConfig.WORKSHOP_A));
    ApiClient.Response created = registerOk(body, AcceptanceConfig.regularTime());
    String number = (String) created.json().get("registrationNumber");

    ApiClient.Response export = api().getAsOrganizer("/api/registrations/export");

    assertThat(export.status()).isEqualTo(200);
    assertThat(export.header("Content-Type"))
        .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    List<List<String>> rows = readSheet(export.raw().body());
    assertThat(rows.get(0)).containsExactlyElementsOf(EXPORT_COLUMNS);
    List<List<String>> matching = rows.stream().filter(r -> r.get(0).equals(number)).toList();
    assertThat(matching).hasSize(1);
    List<String> row = matching.get(0);
    assertThat(row.get(1)).isNotBlank();
    assertThat(row.subList(2, 11))
        .containsExactly(
            (String) body.get("firstName"),
            (String) body.get("lastName"),
            (String) body.get("email"),
            "company",
            (String) body.get("companyName"),
            (String) body.get("companyAddress"),
            (String) body.get("companyVatId"),
            AcceptanceConfig.WORKSHOP_A,
            "no");
    assertThat(row.subList(11, 14))
        .containsExactly(
            created.rawAmount("netFee"), created.rawAmount("vat"), created.rawAmount("grossFee"));
  }

  @Test
  void ac001_05_exportMarksStudents() {
    String number =
        (String)
            registerOk(
                    Registrations.with(Registrations.privatePerson(), "student", true),
                    AcceptanceConfig.earlyBirdTime())
                .json()
                .get("registrationNumber");

    List<String> row =
        exportRows().stream().filter(r -> r.get(0).equals(number)).findFirst().orElseThrow();
    assertThat(row.get(10)).isEqualTo("yes");
  }

  @Test
  void ac001_28_organizerReadsStoredRegistration() {
    Map<String, Object> body = Registrations.company();
    ApiClient.Response created = registerOk(body, AcceptanceConfig.earlyBirdTime());
    String number = (String) created.json().get("registrationNumber");

    ApiClient.Response stored = api().getAsOrganizer("/api/registrations/" + number);

    assertThat(stored.status()).isEqualTo(200);
    Map<String, Object> json = stored.json();
    for (String field :
        List.of(
            "registrationNumber",
            "firstName",
            "lastName",
            "email",
            "payerType",
            "companyName",
            "companyAddress",
            "companyVatId",
            "workshop")) {
      assertThat(json).as(field).containsEntry(field, created.json().get(field));
    }
    for (String amount : List.of("netFee", "vat", "grossFee")) {
      assertThat(stored.rawAmount(amount)).as(amount).isEqualTo(created.rawAmount(amount));
    }
  }

  @Test
  void ac001_29_noCredentialsOrWrongCredentialsGive401WithoutData() {
    Map<String, Object> body = Registrations.privatePerson();
    String number =
        (String)
            registerOk(body, AcceptanceConfig.earlyBirdTime()).json().get("registrationNumber");
    String path = "/api/registrations/" + number;

    List<ApiClient.Response> refused =
        List.of(
            api().get(path, null, null),
            api().get(path, AcceptanceConfig.ORGANIZER_USERNAME, "wrong-password-0000"),
            api().get(path, "someone-else", AcceptanceConfig.ORGANIZER_PASSWORD),
            api().get("/api/registrations/export", null, null),
            api().get("/api/registrations/export", AcceptanceConfig.ORGANIZER_USERNAME, "wrong"));

    for (ApiClient.Response response : refused) {
      assertThat(response.status()).isEqualTo(401);
      assertThat(response.body()).doesNotContain((String) body.get("email"));
      assertThat(response.body()).doesNotContain(number);
    }
  }

  @Test
  void ac001_30_unknownRegistrationNumberGives404() {
    ApiClient.Response response = api().getAsOrganizer("/api/registrations/CR-999999");

    assertThat(response.status()).isEqualTo(404);
  }
}
