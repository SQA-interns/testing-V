package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Workshop selection without capacity check (US-001: AC-001-08). */
class WorkshopAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_08_registrationWithOneWorkshopStoresThatWorkshop() {
    for (String workshopId : workshops().keySet()) {
      Map<String, Object> body = privatePayer(uniqueEmail());
      body.put("workshops", List.of(workshopId));

      JsonNode created = register(body, earlyInstant());

      assertThat(created.get("workshop").asString()).isEqualTo(workshopId);
      JsonNode stored = getAsOrganizer(created.get("registrationNumber").asString()).json();
      assertThat(stored.get("workshop").asString()).isEqualTo(workshopId);
    }
  }

  @Test
  void ac_001_08_registrationWithoutWorkshopStoresNoWorkshop() {
    JsonNode created = register(privatePayer(uniqueEmail()), earlyInstant());

    JsonNode stored = getAsOrganizer(created.get("registrationNumber").asString()).json();
    assertThat(stored.get("workshop").isNull()).as("workshop is null").isTrue();
  }

  @Test
  void ac_001_08_absentWorkshopsFieldMeansNoWorkshop() {
    Map<String, Object> body = privatePayer(uniqueEmail());
    body.remove("workshops");

    JsonNode created = register(body, earlyInstant());

    assertThat(created.get("workshop").isNull()).as("workshop is null").isTrue();
  }

  @Test
  void ac_001_08_noCapacityIsCheckedForAWorkshop() {
    String workshopId = workshops().keySet().iterator().next();
    for (int i = 0; i < 25; i++) {
      Map<String, Object> body = privatePayer(uniqueEmail());
      body.put("workshops", List.of(workshopId));
      JsonNode created = register(body, earlyInstant());
      assertThat(created.get("workshop").asString()).isEqualTo(workshopId);
    }
  }
}
