package si.confreg.registration.web;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.application.RegisterParticipant;
import si.confreg.registration.application.RegistrationQuery;
import tools.jackson.databind.JsonNode;

/** Registration API (contract: registration-api.openapi.yaml, spec 5.1 and 5.2). */
@RestController
public class RegistrationController {

  private final RegisterParticipant registerParticipant;
  private final RegistrationQuery registrationQuery;

  public RegistrationController(
      RegisterParticipant registerParticipant, RegistrationQuery registrationQuery) {
    this.registerParticipant = registerParticipant;
    this.registrationQuery = registrationQuery;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> register(@RequestBody JsonNode body) {
    if (body == null || !body.isObject()) {
      return Problems.response(HttpStatus.BAD_REQUEST);
    }
    RegisterParticipant.Outcome outcome =
        registerParticipant.register(RegistrationRequestReader.read(body));
    return switch (outcome) {
      case RegisterParticipant.Registered registered -> {
        RegistrationResponse response = RegistrationResponse.of(registered.registration());
        yield ResponseEntity.created(
                URI.create("/api/registrations/" + response.registrationNumber()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
      }
      case RegisterParticipant.Rejected rejected -> validationProblem(rejected.errors());
    };
  }

  @GetMapping(path = "/api/registrations/{registrationNumber}")
  public ResponseEntity<Object> get(@PathVariable String registrationNumber) {
    return registrationQuery
        .find(registrationNumber)
        .<ResponseEntity<Object>>map(
            registration ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(RegistrationResponse.of(registration)))
        .orElseGet(() -> Problems.response(HttpStatus.NOT_FOUND));
  }

  private static ResponseEntity<Object> validationProblem(Map<String, String> errors) {
    Map<String, Object> body = Problems.body(HttpStatus.UNPROCESSABLE_CONTENT);
    body.put("title", "Validation failed");
    body.put("errors", errors);
    return ResponseEntity.unprocessableContent().contentType(Problems.PROBLEM_JSON).body(body);
  }
}
