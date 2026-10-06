package si.confreg.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import si.confreg.registration.application.RegistrationRequest;
import tools.jackson.databind.json.JsonMapper;

/** Unit tests of the JSON body mapping (spec 4.2: wrong types per field). */
class RegistrationRequestReaderTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static RegistrationRequest read(String json) {
    return RegistrationRequestReader.read(JSON.readTree(json));
  }

  @Test
  void readsAllFields() {
    RegistrationRequest request =
        read(
            "{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"e\",\"payerType\":\"company\","
                + "\"companyName\":\"N\",\"companyAddress\":\"Ad\",\"companyVatId\":\"V\","
                + "\"workshops\":[\"W\"],\"unknown\":1}");

    assertThat(request.firstName()).isEqualTo("A");
    assertThat(request.lastName()).isEqualTo("B");
    assertThat(request.email()).isEqualTo("e");
    assertThat(request.payerType()).isEqualTo("company");
    assertThat(request.companyName()).isEqualTo("N");
    assertThat(request.companyAddress()).isEqualTo("Ad");
    assertThat(request.companyVatId()).isEqualTo("V");
    assertThat(request.workshops()).containsExactly("W");
    assertThat(request.wrongTypeFields()).isEmpty();
  }

  @Test
  void missingAndNullFieldsAreNull() {
    RegistrationRequest request = read("{\"firstName\":null,\"workshops\":null}");

    assertThat(request.firstName()).isNull();
    assertThat(request.email()).isNull();
    assertThat(request.workshops()).isNull();
    assertThat(request.wrongTypeFields()).isEmpty();
  }

  @Test
  void wrongTypesAreRecordedPerField() {
    RegistrationRequest request =
        read(
            "{\"firstName\":1,\"lastName\":true,\"email\":{\"a\":1},\"payerType\":[\"private\"],"
                + "\"workshops\":\"W\"}");

    assertThat(request.wrongTypeFields())
        .containsExactlyInAnyOrder("firstName", "lastName", "email", "payerType", "workshops");
    assertThat(request.firstName()).isNull();
    assertThat(request.workshops()).isNull();
  }

  @Test
  void workshopElementsMustBeStrings() {
    assertThat(read("{\"workshops\":[1]}").wrongTypeFields()).containsExactly("workshops");
    assertThat(read("{\"workshops\":[null]}").workshops()).containsExactly((String) null);
    assertThat(read("{\"workshops\":[]}").workshops()).isEmpty();
  }
}
