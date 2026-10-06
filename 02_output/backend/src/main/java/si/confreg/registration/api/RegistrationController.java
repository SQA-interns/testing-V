package si.confreg.registration.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;
import si.confreg.registration.application.RegistrationService;
import si.confreg.registration.domain.Registration;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Registration API (contract {@code registration-api.openapi.yaml}). */
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

  private static final TypeReference<Map<String, Object>> BODY = new TypeReference<>() {};

  private final RegistrationService service;
  private final ObjectMapper json;

  public RegistrationController(RegistrationService service, ObjectMapper json) {
    this.service = service;
    this.json = json;
  }

  /** Public: submit a registration (AC-001-01 to 09). */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RegistrationResponse> create(@RequestBody JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new NotAJsonObjectException();
    }
    Registration registration = service.register(json.convertValue(body, BODY));
    URI location =
        URI.create(
            "/api/registrations/"
                + UriUtils.encodePathSegment(
                    registration.registrationNumber(), StandardCharsets.UTF_8));
    return ResponseEntity.created(location).body(RegistrationResponse.of(registration));
  }

  /** Organizer only: read one registration (AC-001-10 to 12). */
  @GetMapping("/{registrationNumber}")
  public RegistrationResponse get(@PathVariable String registrationNumber) {
    return service
        .find(registrationNumber)
        .map(RegistrationResponse::of)
        .orElseThrow(RegistrationNotFoundException::new);
  }
}
