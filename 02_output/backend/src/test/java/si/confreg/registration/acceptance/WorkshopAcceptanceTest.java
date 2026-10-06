package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** US-001 AC-001-07: optional single workshop from configuration, no fee effect (D-22, D-28). */
class WorkshopAcceptanceTest extends AcceptanceTestBase {

  @Test
  void AC_001_07_chosenConfiguredWorkshopIsStored() {
    String workshop = workshopIds().get(1);
    Map<String, Object> body = privateRegistration(uniqueEmail("ws"));
    body.put("workshops", List.of(workshop));

    HttpResponse<String> created = postRegistration(body, deadlineDayStart());

    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
    assertThat(json(created, "$.workshop")).isEqualTo(workshop);
    HttpResponse<String> read =
        getRegistration((String) json(created, "$.registrationNumber"), true);
    assertThat(json(read, "$.workshop")).isEqualTo(workshop);
  }

  @Test
  void AC_001_07_noWorkshopIsStoredAsNull() {
    Map<String, Object> withEmptyList = privateRegistration(uniqueEmail("ws-none"));
    Map<String, Object> withoutField = privateRegistration(uniqueEmail("ws-absent"));
    withoutField.remove("workshops");
    Map<String, Object> withNull = privateRegistration(uniqueEmail("ws-null"));
    withNull.put("workshops", null);

    for (Map<String, Object> body : List.of(withEmptyList, withoutField, withNull)) {
      HttpResponse<String> created = postRegistration(body, deadlineDayStart());
      assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
      assertThat(json(created, "$.workshop")).isNull();
    }
  }

  @Test
  void AC_001_07_workshopDoesNotChangeTheFee() {
    Map<String, Object> body = privateRegistration(uniqueEmail("ws-fee"));
    body.put("workshops", List.of(workshopIds().get(0)));

    HttpResponse<String> created = postRegistration(body, deadlineDayStart());

    assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
    assertThat(amount(created, "netFee")).isEqualByComparingTo(config("APP_FEE_EARLY"));
  }

  @Test
  void AC_001_07_configuredWorkshopsAreListedPublicly() {
    HttpResponse<String> response =
        send(HttpRequest.newBuilder(URI.create(baseUrl() + "/api/workshops")).GET().build());

    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    List<String> ids = com.jayway.jsonpath.JsonPath.read(response.body(), "$[*].id");
    List<String> titles = com.jayway.jsonpath.JsonPath.read(response.body(), "$[*].title");
    assertThat(ids).containsExactlyElementsOf(workshopIds());
    assertThat(titles)
        .containsExactlyElementsOf(
            workshopIds().stream().map(AcceptanceTestBase::workshopTitle).toList());
  }
}
