package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** AC-001-08 workshop selection; AC-001-11 (API part) public list of configured workshops. */
class WorkshopAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-001-08 each configured workshop can be selected and is stored")
  void ac001_08_selectedWorkshopIsStored() {
    for (String id : workshopIds()) {
      Map<String, Object> body = anaPrivate();
      body.put("workshops", List.of(id));

      ApiResponse created = registerSuccessfully(body, daysFromDeadline(-4));

      assertThat(created.json().path("workshop").asString("")).isEqualTo(id);
      JsonNode stored = storedRegistration(registrationNumberOf(created)).json();
      assertThat(stored.path("workshop").asString("")).as("stored workshop").isEqualTo(id);
    }
  }

  @Test
  @DisplayName("AC-001-08 no workshop selected (empty list) is stored as no workshop")
  void ac001_08_emptyWorkshopListStoresNoWorkshop() {
    ApiResponse created = registerSuccessfully(anaPrivate(), daysFromDeadline(-4));

    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();
    assertThat(stored.has("workshop")).isTrue();
    assertThat(stored.path("workshop").isNull()).as("workshop is null").isTrue();
  }

  @Test
  @DisplayName("AC-001-08 workshops omitted is stored as no workshop")
  void ac001_08_omittedWorkshopsStoresNoWorkshop() {
    Map<String, Object> body = anaPrivate();
    body.remove("workshops");

    ApiResponse created = registerSuccessfully(body, daysFromDeadline(-4));

    JsonNode stored = storedRegistration(registrationNumberOf(created)).json();
    assertThat(stored.path("workshop").isNull()).as("workshop is null").isTrue();
  }

  @Test
  @DisplayName("AC-001-08 no capacity is checked: many registrations for one workshop succeed")
  void ac001_08_noCapacityCheck() {
    String id = workshopIds().get(0);
    for (int i = 0; i < 25; i++) {
      Map<String, Object> body = privatePayer("Ana", "Novak", "p" + i + "@example.org");
      body.put("workshops", List.of(id));
      ApiResponse created = registerSuccessfully(body, daysFromDeadline(-4));
      assertThat(created.json().path("workshop").asString("")).isEqualTo(id);
    }
  }

  @Test
  @DisplayName("AC-001-11 public workshop list returns the configured workshops in order")
  void ac001_11_publicWorkshopListMatchesConfiguration() {
    ApiResponse response = getWorkshops();

    assertThat(response.status()).as("status, body: %s", response.body()).isEqualTo(200);
    List<String> ids = new ArrayList<>();
    for (JsonNode workshop : response.json()) {
      String id = workshop.path("id").asString("");
      ids.add(id);
      assertThat(workshop.path("title").asString("")).isEqualTo(workshops().get(id));
    }
    assertThat(ids).containsExactlyElementsOf(workshopIds());
  }
}
