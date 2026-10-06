package si.confreg.registration.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import si.confreg.registration.application.RegistrationCommand;

class RegistrationRequestMapperTest {

  @Test
  void mapsValuesOfTheRightType() {
    Map<String, Object> body = new HashMap<>();
    body.put("firstName", "Ana");
    body.put("email", "ana@example.org");
    body.put("workshops", List.of("W1"));
    body.put("student", true);

    RegistrationCommand command = RegistrationRequestMapper.toCommand(body);

    assertThat(command.firstName()).isEqualTo("Ana");
    assertThat(command.lastName()).isNull();
    assertThat(command.workshops()).containsExactly("W1");
    assertThat(command.student()).isTrue();
    assertThat(command.typeErrors()).isEmpty();
  }

  @Test
  void wrongTypesBecomeFieldErrors() {
    Map<String, Object> body = new HashMap<>();
    body.put("firstName", 42);
    body.put("payerType", List.of("private"));
    body.put("workshops", "W1");
    body.put("student", "yes");

    RegistrationCommand command = RegistrationRequestMapper.toCommand(body);

    assertThat(command.typeErrors())
        .containsExactlyInAnyOrder("firstName", "payerType", "workshops", "student");
    assertThat(command.firstName()).isNull();
    assertThat(command.workshops()).isNull();
    assertThat(command.student()).isFalse();
  }

  @Test
  void nonStringWorkshopIdIsATypeError() {
    Map<String, Object> body = new HashMap<>();
    body.put("workshops", List.of(1));

    assertThat(RegistrationRequestMapper.toCommand(body).typeErrors()).containsExactly("workshops");
  }

  @Test
  void missingStudentIsFalse() {
    assertThat(RegistrationRequestMapper.toCommand(Map.of()).student()).isFalse();
  }
}
