package si.confreg.registration.api;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.domain.Registration;
import si.confreg.registration.service.RegistrationService;

/** Fixed registration API (project/02_design/architecture.md). */
@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

  private final RegistrationService service;

  public RegistrationController(RegistrationService service) {
    this.service = service;
  }

  /** Public: registers a participant (US-001). */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
    Registration registration = service.register(request.toInput());
    return ResponseEntity.created(
            URI.create("/api/registrations/" + registration.getRegistrationNumber()))
        .body(RegistrationResponse.of(registration));
  }

  /** Organizer only: reads a stored registration (AC-001-04, AC-001-06). */
  @GetMapping("/{registrationNumber}")
  public ResponseEntity<RegistrationResponse> get(@PathVariable String registrationNumber) {
    return service
        .find(registrationNumber)
        .map(registration -> ResponseEntity.ok(RegistrationResponse.of(registration)))
        .orElseThrow(RegistrationNotFoundException::new);
  }
}
