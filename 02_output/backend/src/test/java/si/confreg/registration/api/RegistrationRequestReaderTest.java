package si.confreg.registration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class RegistrationRequestReaderTest {

  private final RegistrationRequestReader reader =
      new RegistrationRequestReader(JsonMapper.builder().build());

  @Test
  void readsAllKnownFields() {
    RegistrationRequestReader.Parsed parsed =
        reader.read(
            """
            {"firstName":"Špela","lastName":"Žagar","email":"s@example.com","payerType":"company",
             "companyName":"X","companyAddress":"Y","companyVatId":"SI1","workshops":["W1"]}
            """);

    assertThat(parsed.unacceptedFields()).isEmpty();
    assertThat(parsed.input().firstName()).isEqualTo("Špela");
    assertThat(parsed.input().companyVatId()).isEqualTo("SI1");
    assertThat(parsed.input().workshops()).containsExactly("W1");
  }

  @Test
  void nullAndAbsentValuesAreNull() {
    RegistrationRequestReader.Parsed parsed =
        reader.read("{\"firstName\":null,\"workshops\":null}");

    assertThat(parsed.unacceptedFields()).isEmpty();
    assertThat(parsed.input().firstName()).isNull();
    assertThat(parsed.input().lastName()).isNull();
    assertThat(parsed.input().workshops()).isNull();
  }

  @Test
  void unknownPropertiesAndWrongTypesAreReported() {
    RegistrationRequestReader.Parsed parsed =
        reader.read(
            "{\"firstName\":1,\"email\":[\"a\"],\"workshops\":\"W1\",\"discount\":\"FREE\"}");

    assertThat(parsed.unacceptedFields())
        .containsExactlyInAnyOrder("firstName", "email", "workshops", "discount");
  }

  @Test
  void workshopElementsMustBeStrings() {
    assertThat(reader.read("{\"workshops\":[\"W1\",2]}").unacceptedFields())
        .isEqualTo(List.of("workshops"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{", "[]", "\"text\"", "42", "null", "{\"a\":}"})
  void nonObjectOrMalformedBodyIsMalformed(String body) {
    assertThatThrownBy(() -> reader.read(body)).isInstanceOf(MalformedRequestException.class);
  }
}
