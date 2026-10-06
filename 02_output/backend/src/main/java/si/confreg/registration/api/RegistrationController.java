package si.confreg.registration.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.confreg.registration.application.FindRegistration;
import si.confreg.registration.application.RegisterParticipant;
import si.confreg.registration.domain.Registration;

/** Fixed registration API (architecture.md; docs/02_contracts/registration-api.openapi.yaml). */
@RestController
@RequestMapping(path = "/api/registrations", produces = MediaType.APPLICATION_JSON_VALUE)
class RegistrationController {

  private static final Pattern NUMBER = Pattern.compile("^REG-[0-9]{6,}$");

  private final RegistrationRequestReader reader;
  private final RegisterParticipant registerParticipant;
  private final FindRegistration findRegistration;

  RegistrationController(
      RegistrationRequestReader reader,
      RegisterParticipant registerParticipant,
      FindRegistration findRegistration) {
    this.reader = reader;
    this.registerParticipant = registerParticipant;
    this.findRegistration = findRegistration;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<RegistrationResponse> register(@RequestBody byte[] body) {
    RegistrationRequestReader.Parsed parsed = reader.read(new String(body, StandardCharsets.UTF_8));
    Registration registration =
        registerParticipant.register(parsed.input(), parsed.unacceptedFields());
    return ResponseEntity.created(
            URI.create("/api/registrations/" + registration.registrationNumber()))
        .body(RegistrationResponse.from(registration));
  }

  @GetMapping("/{registrationNumber}")
  RegistrationResponse get(@PathVariable String registrationNumber) {
    if (!NUMBER.matcher(registrationNumber).matches()) {
      throw new NotFoundException();
    }
    return findRegistration
        .find(registrationNumber)
        .map(RegistrationResponse::from)
        .orElseThrow(NotFoundException::new);
  }
}
