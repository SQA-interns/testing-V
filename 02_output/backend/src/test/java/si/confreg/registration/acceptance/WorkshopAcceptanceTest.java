package si.confreg.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-001 added workshop criteria AC-001-14 to AC-001-16 (D-20). */
class WorkshopAcceptanceTest extends AcceptanceTestBase {

  @Test
  void ac_001_14_eachConfiguredWorkshopIsStored() {
    for (String id : workshopIds()) {
      ObjectNode body = privateRegistration();
      body.putArray("workshops").add(id);

      JsonNode registration = completed(register(body, lastEarlyInstant()));

      assertThat(registration.get("workshop").asString()).as(id).isEqualTo(id);
    }
  }

  @Test
  void ac_001_15_absentWorkshopsStoresNull() {
    ObjectNode body = privateRegistration();
    body.remove("workshops");

    JsonNode registration = completed(register(body, lastEarlyInstant()));

    assertThat(registration.get("workshop").isNull()).isTrue();
  }

  @Test
  void ac_001_15_emptyWorkshopsStoresNull() {
    ObjectNode body = companyRegistration();
    body.putArray("workshops");

    JsonNode registration = completed(register(body, lastEarlyInstant()));

    assertThat(registration.get("workshop").isNull()).isTrue();
  }

  @Test
  void ac_001_16_unknownWorkshopRejected() {
    String unknown = "X" + String.join("", workshopIds());
    ObjectNode body = privateRegistration();
    body.putArray("workshops").add(unknown);

    assertRejected(body);
  }

  @Test
  void ac_001_16_moreThanOneWorkshopRejected() {
    List<String> ids = workshopIds();
    assertThat(ids).hasSizeGreaterThanOrEqualTo(2);
    ObjectNode body = privateRegistration();
    body.putArray("workshops").add(ids.get(0)).add(ids.get(1));

    assertRejected(body);
  }

  @Test
  void ac_001_16_sameWorkshopTwiceRejected() {
    ObjectNode body = privateRegistration();
    body.putArray("workshops").add(workshopIds().get(0)).add(workshopIds().get(0));

    assertRejected(body);
  }
}
